package com.inkspire.rogue.core;

import com.inkspire.rogue.ai.EnemyAi;
import com.inkspire.rogue.content.PotionLibrary;
import com.inkspire.rogue.content.RelicLibrary;
import com.inkspire.rogue.model.CardInstance;
import com.inkspire.rogue.model.CardType;
import com.inkspire.rogue.model.Effect;
import com.inkspire.rogue.model.MoveDef;
import com.inkspire.rogue.model.PotionDef;
import com.inkspire.rogue.model.RelicDef;
import com.inkspire.rogue.model.Status;
import com.inkspire.rogue.model.Target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic, animation-free combat engine. Every public action mutates state once and
 * returns; the view redraws a single frame afterwards.
 *
 * <p>伤害结算顺序（与《杀戮尖塔》一致，逐次向下取整）：
 * <pre>
 *   玩家打敌人： (基础伤害 + 力量×倍率 + 遗物加伤)
 *              → 虚弱 ×0.75 → 易伤 ×1.5 → 笔尖 ×2 → 不低于 0 → 扣格挡
 *   敌人打玩家： (基础伤害 + 敌人力量) → 敌人虚弱 ×0.75 → 玩家易伤 ×1.5 → 不低于 0 → 扣格挡
 * </pre>
 */
public final class CombatEngine {
    public final CombatState c;
    private int damageThisCard;
    private int killsThisCard;
    private int xSpent;

    public CombatEngine(CombatState state) { this.c = state; }

    public void start() {
        c.turn = 1;
        c.playerTurn = true;
        c.energy = c.maxEnergy + c.rFirstTurnEnergy;
        applyCombatStartRelics();
        if (c.freeFirstPowerOwned) c.necronomiconReady = true;
        chooseIntents();
        c.draw(5 + c.rStartDraw);
        c.log("战斗开始。");
        if (Cheats.autoWin) endCombat(true);
    }

    private void applyCombatStartRelics() {
        for (String id : c.run.relics) {
            RelicDef d = RelicLibrary.get(id);
            if (d == null) continue;
            if (d.startBlock > 0) {
                c.block += d.startBlock;
                c.log("「" + d.name + "」获得 " + d.startBlock + " 点格挡。");
            }
            if (d.startStrength != 0) c.addPlayerStatus(Status.STRENGTH, d.startStrength);
            if (d.startDexterity != 0) c.addPlayerStatus(Status.DEXTERITY, d.startDexterity);
            if (d.startThorns != 0) c.addPlayerStatus(Status.THORNS, d.startThorns);
            if (d.startPoison > 0) {
                for (EnemyState e : c.living()) e.addStatus(Status.POISON, d.startPoison);
            }
            if (d.loseHpStart > 0) loseHp(d.loseHpStart, true);
        }
    }

    public boolean has(String relic) { return c.run.hasRelic(relic); }

    public int currentCost(CardInstance card) {
        if (card.def.xCost) return -1;
        if (Cheats.freeCards) return 0;
        if (c.playerStatus(Status.CORRUPTION) > 0 && card.def.type == CardType.SKILL) return 0;
        if (c.necronomiconReady && card.def.type == CardType.POWER) return 0;
        return Math.max(0, card.cost());
    }

    public boolean canPlay(CardInstance card) {
        if (card == null || c.over || !c.playerTurn) return false;
        if (!c.hand.contains(card)) return false;
        if (card.def.type == CardType.CURSE || card.def.type == CardType.STATUS) return false;
        if (card.def.xCost) return true;
        return Cheats.infiniteEnergy || currentCost(card) <= c.energy;
    }

    public void play(CardInstance card, EnemyState selectedTarget) {
        if (!canPlay(card)) return;
        EnemyState target = selectedTarget;
        if (target == null || !target.alive()) target = c.firstLiving();
        if (card.def.target == Target.ENEMY && target == null) return;

        int cost = currentCost(card);
        xSpent = 0;
        if (card.def.xCost) {
            xSpent = Cheats.infiniteEnergy ? Math.max(1, c.energy) : c.energy;
            if (!Cheats.infiniteEnergy) c.energy = 0;
        } else if (!Cheats.infiniteEnergy) {
            c.energy = Math.max(0, c.energy - cost);
        }
        if (c.necronomiconReady && card.def.type == CardType.POWER) c.necronomiconReady = false;

        c.hand.remove(card);
        c.cardsPlayedTotal++;
        c.cardsPlayedThisTurn++;
        c.hintUid = "";
        damageThisCard = 0;
        killsThisCard = 0;
        boolean isAttack = card.def.type == CardType.ATTACK;
        if (isAttack) c.attacksPlayed++;
        boolean penNib = isAttack && c.penNibReady;
        if (penNib) { c.penNibReady = false; c.penNibCount = 0; }
        int repeats = isAttack && c.playerStatus(Status.DOUBLE_TAP) > 0 ? 2 : 1;
        if (repeats == 2) c.addPlayerStatus(Status.DOUBLE_TAP, -1);

        for (int r = 0; r < repeats; r++) {
            for (Effect e : card.effects()) resolveCardEffect(e, card, target, penNib);
            if (c.hp <= 0 || c.over) break;
        }

        if (card.def.healUnblocked && damageThisCard > 0) {
            int before = c.hp;
            c.hp = Math.min(c.maxHp, c.hp + damageThisCard);
            int gained = c.hp - before;
            if (gained > 0) c.log("收割回复 " + gained + " 生命。");
        }
        if (card.def.onKillMaxHp > 0 && killsThisCard > 0) {
            c.maxHp += card.def.onKillMaxHp * killsThisCard;
            c.hp += card.def.onKillMaxHp * killsThisCard;
            c.log("最大生命 +" + (card.def.onKillMaxHp * killsThisCard) + "。");
        }
        if (isAttack) {
            if (c.penNibOwned) {
                c.penNibCount++;
                if (c.penNibCount >= 10) { c.penNibReady = true; c.penNibCount = 0; }
            }
            if (c.rCardPoison > 0 && target != null && target.alive()) {
                target.addStatus(Status.POISON, c.rCardPoison);
            }
        }

        if (card.def.type == CardType.POWER) {
            c.powers.add(card);
        } else if (card.exhaust() || (c.playerStatus(Status.CORRUPTION) > 0 && card.def.type == CardType.SKILL)) {
            // 腐化：技能牌打出后被消耗
            exhaust(card);
        } else {
            c.discardPile.add(card);
        }
        if (card.def.addCopyToDiscard && c.discardPile.size() < 60) c.discardPile.add(card.copy());
        if (card.noDraw()) c.noDraw = true;
        if (c.rCardBlock > 0) gainBlock(c.rCardBlock);

        c.log("打出「" + card.name() + "」。");
        if (c.hp <= 0) { endCombat(false); return; }
        if (c.living().isEmpty()) { endCombat(true); return; }
        if (Cheats.autoWin) endCombat(true);
    }

    private void resolveCardEffect(Effect e, CardInstance card, EnemyState target, boolean penNib) {
        List<EnemyState> alive = c.living();
        switch (e.kind) {
            case DAMAGE:
                for (int hit = 0; hit < e.hits; hit++) {
                    if (target == null || !target.alive()) break;
                    attackEnemy(target, e.amount, e.strengthMultiplier, penNib);
                }
                break;
            case DAMAGE_ALL:
                for (int hit = 0; hit < e.hits; hit++) {
                    for (EnemyState en : new ArrayList<>(c.living())) attackEnemy(en, e.amount, e.strengthMultiplier, penNib);
                }
                break;
            case DAMAGE_RANDOM:
                for (int hit = 0; hit < e.hits; hit++) {
                    List<EnemyState> live = c.living();
                    if (live.isEmpty()) break;
                    attackEnemy(live.get(c.rng.nextInt(live.size())), e.amount, e.strengthMultiplier, penNib);
                }
                break;
            case DAMAGE_X:
                for (int hit = 0; hit < xSpent; hit++) {
                    for (EnemyState en : new ArrayList<>(c.living())) attackEnemy(en, e.amount, e.strengthMultiplier, penNib);
                }
                break;
            case DAMAGE_BLOCK:
                if (target != null && target.alive()) attackEnemy(target, c.block, 1, penNib);
                break;
            case BLOCK:
                gainBlock(e.amount);
                break;
            case DRAW:
                c.draw(e.amount);
                break;
            case ENERGY:
                c.energy += e.amount;
                break;
            case HEAL:
                int before = c.hp;
                c.hp = Math.min(c.maxHp, c.hp + e.amount);
                if (c.hp > before) c.log("回复 " + (c.hp - before) + " 生命。");
                break;
            case LOSE_HP:
                loseHp(e.amount, true);
                break;
            case MAX_HP:
                c.maxHp += e.amount;
                c.hp += e.amount;
                break;
            case APPLY_ENEMY:
                if (target != null && target.alive()) applyToEnemy(target, e.status, e.amount);
                break;
            case APPLY_ALL:
                for (EnemyState en : alive) if (en.alive()) applyToEnemy(en, e.status, e.amount);
                break;
            case POWER:
                c.addPlayerStatus(e.status, e.amount);
                c.log("获得「" + e.status.label + " " + e.amount + "」。");
                break;
            case DOUBLE_STRENGTH:
                int s = c.playerStatus(Status.STRENGTH);
                if (s > 0) {
                    c.addPlayerStatus(Status.STRENGTH, s);
                    c.log("力量翻倍。");
                }
                break;
            case DOUBLE_BLOCK:
                if (c.block > 0) {
                    c.block *= 2;
                    c.log("格挡翻倍。");
                }
                break;
            case EXHAUST_RANDOM:
                for (int i = 0; i < e.amount && !c.hand.isEmpty(); i++) {
                    CardInstance victim = c.hand.remove(c.rng.nextInt(c.hand.size()));
                    exhaust(victim);
                }
                break;
            case EXHAUST_NON_ATTACK_BLOCK:
                ArrayList<CardInstance> remove = new ArrayList<>();
                for (CardInstance h : c.hand) if (h.def.type != CardType.ATTACK) remove.add(h);
                for (CardInstance h : remove) { c.hand.remove(h); exhaust(h); gainBlock(e.amount); }
                break;
            default:
                break;
        }
    }

    /* ==================== 玩家 → 敌人 ==================== */

    public void attackEnemy(EnemyState target, int base, int strMultiplier, boolean penNib) {
        if (target == null || !target.alive()) return;
        int dmg = base + c.playerStatus(Status.STRENGTH) * Math.max(1, strMultiplier) + c.rAttackBonus;
        if (c.playerStatus(Status.WEAK) > 0) dmg = (int) Math.floor(dmg * 0.75);
        if (target.status(Status.VULNERABLE) > 0) dmg = (int) Math.floor(dmg * 1.5);
        if (penNib) dmg *= 2;
        if (Cheats.oneHit) dmg = 9999;
        dmg = Math.max(0, dmg);
        int blocked = Math.min(target.block, dmg);
        target.block -= blocked;
        int hpLoss = dmg - blocked;
        if (hpLoss > 0) {
            target.damage(hpLoss);
            damageThisCard += hpLoss;
            c.totalDamageDealt += hpLoss;
            c.log("对 " + target.def.name + " 造成 " + hpLoss + " 伤害。");
        } else if (blocked > 0) {
            c.log(target.def.name + " 的格挡吸收了 " + blocked + " 点伤害。");
        }
        if (!target.alive()) {
            killsThisCard++;
            if (!c.shadow) c.run.kills++;
            c.log(target.def.name + " 被击倒。");
        }
        int thorns = target.status(Status.THORNS);
        if (thorns > 0 && !Cheats.invincible) {
            c.hp = Math.max(0, c.hp - thorns);
            c.totalDamageTaken += thorns;
            c.log(target.def.name + " 的荆棘反伤 " + thorns + "。");
        }
    }

    public void gainBlock(int amount) {
        int gain = amount + c.playerStatus(Status.DEXTERITY);
        if (c.playerStatus(Status.FRAIL) > 0) gain = (int) Math.floor(gain * 0.75);
        gain = Math.max(0, gain);
        if (gain <= 0) return;
        c.block += gain;
        if (c.playerStatus(Status.JUGGERNAUT) > 0) {
            List<EnemyState> live = c.living();
            if (!live.isEmpty()) {
                EnemyState t = live.get(c.rng.nextInt(live.size()));
                int d = c.playerStatus(Status.JUGGERNAUT);
                int loss = Math.min(t.block, d);
                t.block -= loss;
                int rest = d - loss;
                if (rest > 0) { t.damage(rest); c.totalDamageDealt += rest; }
                c.log("势不可挡：" + t.def.name + " 受到 " + rest + " 伤害。");
                if (!t.alive()) { if (!c.shadow) c.run.kills++; c.log(t.def.name + " 被击倒。"); }
            }
        }
    }

    public void loseHp(int amount, boolean triggerRupture) {
        if (amount <= 0) return;
        if (Cheats.invincible) return;
        c.hp = Math.max(0, c.hp - amount);
        c.totalDamageTaken += amount;
        if (triggerRupture && c.playerStatus(Status.RUPTURE) > 0) {
            int r = c.playerStatus(Status.RUPTURE);
            c.addPlayerStatus(Status.STRENGTH, r);
            c.log("撕裂：力量 +" + r + "。");
        }
        c.log("失去 " + amount + " 生命。");
    }

    private void applyToEnemy(EnemyState e, Status s, int amount) {
        if (s == Status.SHACKLE) {
            int n = Math.max(0, amount);
            if (n == 0) return;
            e.addStatus(Status.STRENGTH, -n);
            e.addStatus(Status.SHACKLE, n);
            c.log(e.def.name + " 力量 -" + n + "。");
            return;
        }
        e.addStatus(s, amount);
        if (s == Status.STRENGTH && amount < 0) c.log(e.def.name + " 力量 " + amount + "。");
        else if (s == Status.POISON && amount > 0) c.log(e.def.name + " 中毒 +" + amount + "。");
        else if ((s == Status.WEAK || s == Status.VULNERABLE || s == Status.FRAIL) && amount > 0) {
            c.log(e.def.name + " 获得 " + s.label + " " + amount + "。");
        }
    }

    private void exhaust(CardInstance card) {
        c.exhaustPile.add(card);
        if (c.playerStatus(Status.FEEL_NO_PAIN) > 0) gainBlock(c.playerStatus(Status.FEEL_NO_PAIN));
        if (c.playerStatus(Status.DARK_EMBRACE) > 0) c.draw(c.playerStatus(Status.DARK_EMBRACE));
    }

    /* ==================== 回合流转 ==================== */

    public void endTurn() {
        if (c.over || !c.playerTurn) return;
        c.playerTurn = false;

        // 1) 手牌：保留 / 虚无 / 弃掉；诅咒牌结算
        ArrayList<CardInstance> keep = new ArrayList<>();
        for (CardInstance h : new ArrayList<>(c.hand)) {
            if (h.def.handEndTurnLoseHp > 0) {
                loseHp(h.def.handEndTurnLoseHp, false);
                c.log("「" + h.name() + "」在手中灼烧。");
            }
            if (h.def.retain) keep.add(h);
            else if (h.def.ethereal) exhaust(h);
            else c.discardPile.add(h);
        }
        c.hand.clear();
        c.hand.addAll(keep);

        // 2) 玩家回合结束时递减的减益
        tickPlayer(Status.VULNERABLE);
        tickPlayer(Status.WEAK);
        tickPlayer(Status.FRAIL);
        tickPlayer(Status.DOUBLE_TAP);

        // 3) 回合结束触发的格挡/回血（在敌人行动前生效）
        if (c.playerStatus(Status.METALLICIZE) > 0) gainBlock(c.playerStatus(Status.METALLICIZE));
        if (c.rBlockPerTurn > 0) gainBlock(c.rBlockPerTurn);
        if (c.playerStatus(Status.REGEN) > 0) {
            int r = c.playerStatus(Status.REGEN);
            int before = c.hp;
            c.hp = Math.min(c.maxHp, c.hp + r);
            if (c.hp > before) c.log("再生回复 " + (c.hp - before) + "。");
        }

        // 4) 敌人行动
        for (EnemyState e : c.enemies) {
            if (!e.alive()) continue;
            if (c.over) break;
            e.block = 0;

            // 中毒：在自己的回合开始时失去生命，无视格挡，再减一层
            int poison = e.status(Status.POISON);
            if (poison > 0) {
                e.damage(poison);
                e.addStatus(Status.POISON, -1);
                c.log(e.def.name + " 中毒失去 " + poison + " 生命。");
                if (!e.alive()) { if (!c.shadow) c.run.kills++; c.log(e.def.name + " 死于中毒。"); continue; }
            }
            if (e.status(Status.RITUAL) > 0) e.addStatus(Status.STRENGTH, e.status(Status.RITUAL));
            if (e.status(Status.REGEN) > 0) e.heal(e.status(Status.REGEN));

            if (e.intent != null) {
                MoveDef intent = e.intent;
                enemyAct(e, intent);
                e.recordMove(intent, c.turn);
            }
            // 枷锁：回合结束时把借走的力量还回去
            int shackle = e.status(Status.SHACKLE);
            if (shackle > 0) {
                e.addStatus(Status.STRENGTH, shackle);
                e.addStatus(Status.SHACKLE, -shackle);
            }
            tickEnemy(e, Status.VULNERABLE);
            tickEnemy(e, Status.WEAK);
            tickEnemy(e, Status.FRAIL);
            if (c.hp <= 0) break;
        }
        if (c.hp <= 0) { endCombat(false); return; }
        if (c.living().isEmpty()) { endCombat(true); return; }

        // 5) 回合结束的一次性状态
        if (c.playerStatus(Status.FLAME_BARRIER) > 0) tickPlayer(Status.FLAME_BARRIER);
        if (c.playerStatus(Status.BARRICADE) <= 0) c.block = 0;

        // 6) 新回合开始
        c.turn++;
        c.playerTurn = true;
        c.noDraw = false;
        c.energy = c.maxEnergy + c.playerStatus(Status.BERSERK) + c.rEnergyPerTurn;
        if (c.playerStatus(Status.DEMON_FORM) > 0) c.addPlayerStatus(Status.STRENGTH, c.playerStatus(Status.DEMON_FORM));
        if (c.rStrengthPerTurn > 0) c.addPlayerStatus(Status.STRENGTH, c.rStrengthPerTurn);
        if (c.rDexterityPerTurn > 0) c.addPlayerStatus(Status.DEXTERITY, c.rDexterityPerTurn);

        // 玩家中毒
        int selfPoison = c.playerStatus(Status.POISON);
        if (selfPoison > 0) {
            loseHp(selfPoison, false);
            c.addPlayerStatus(Status.POISON, -1);
            if (c.hp <= 0) { endCombat(false); return; }
        }
        if (c.playerStatus(Status.BRUTALITY) > 0) {
            loseHp(1, true);
            c.draw(1);
        }
        if (c.playerStatus(Status.REGEN) > 0) c.hp = Math.min(c.maxHp, c.hp + c.playerStatus(Status.REGEN));
        // 诅咒牌：回合开始时若仍在手牌
        for (CardInstance h : new ArrayList<>(c.hand)) {
            if (h.def.handTurnStartWeak > 0) {
                c.addPlayerStatus(Status.WEAK, h.def.handTurnStartWeak);
                c.log("「" + h.name() + "」让你虚弱 " + h.def.handTurnStartWeak + "。");
            }
        }
        c.draw(5 + c.rDrawPerTurn);
        chooseIntents();
        c.log("—— 第 " + c.turn + " 回合 ——");
        if (c.hp <= 0) endCombat(false);
    }

    private void enemyAct(EnemyState e, MoveDef m) {
        if (m == null) return;
        c.log(e.def.name + " 使用「" + m.name + "」。");
        for (Effect ef : m.effects) {
            if (c.hp <= 0) break;
            switch (ef.kind) {
                case DAMAGE:
                case DAMAGE_ALL:
                    for (int i = 0; i < ef.hits; i++) enemyHitPlayer(e, ef.amount);
                    break;
                case BLOCK:
                    int b = Math.max(0, ef.amount + e.status(Status.DEXTERITY));
                    if (e.status(Status.FRAIL) > 0) b = (int) Math.floor(b * 0.75);
                    e.block += b;
                    c.log(e.def.name + " 获得 " + b + " 格挡。");
                    break;
                case HEAL:
                    e.heal(ef.amount);
                    c.log(e.def.name + " 回复 " + ef.amount + "。");
                    break;
                case APPLY_ENEMY:
                case APPLY_ALL:
                    c.addPlayerStatus(ef.status, ef.amount);
                    c.log("你获得「" + ef.status.label + " " + ef.amount + "」。");
                    break;
                case POWER:
                    e.addStatus(ef.status, ef.amount);
                    c.log(e.def.name + " 获得「" + ef.status.label + " " + ef.amount + "」。");
                    break;
                case LOSE_HP:
                    e.hp = Math.max(0, e.hp - ef.amount);
                    c.log(e.def.name + " 失去 " + ef.amount + " 生命。");
                    break;
                default:
                    break;
            }
        }
    }

    private void enemyHitPlayer(EnemyState e, int base) {
        int dmg = e.scaledDamage(base) + e.status(Status.STRENGTH);
        if (e.status(Status.WEAK) > 0) dmg = (int) Math.floor(dmg * 0.75);
        if (c.playerStatus(Status.VULNERABLE) > 0) dmg = (int) Math.floor(dmg * 1.5);
        dmg = Math.max(0, dmg);
        if (Cheats.invincible) dmg = 0;
        int blocked = Math.min(c.block, dmg);
        c.block -= blocked;
        int hpLoss = dmg - blocked;
        if (hpLoss > 0) {
            c.hp = Math.max(0, c.hp - hpLoss);
            c.totalDamageTaken += hpLoss;
            c.log("你受到 " + hpLoss + " 伤害。");
        } else {
            c.log("格挡挡下了这次攻击。");
        }
        int thorns = c.playerStatus(Status.THORNS) + c.playerStatus(Status.FLAME_BARRIER);
        if (thorns > 0 && e.alive()) {
            int loss = Math.min(e.block, thorns);
            e.block -= loss;
            int rest = thorns - loss;
            if (rest > 0) e.damage(rest);
            c.log("荆棘反伤 " + thorns + "。");
            if (e.hp <= 0) { if (!c.shadow) c.run.kills++; c.log(e.def.name + " 被反伤击倒。"); }
        }
    }

    private void tickPlayer(Status s) {
        if (c.playerStatuses.containsKey(s)) {
            int v = c.playerStatus(s) - 1;
            if (v <= 0) c.playerStatuses.remove(s); else c.playerStatuses.put(s, v);
        }
    }

    private void tickEnemy(EnemyState e, Status s) {
        if (e.statuses.containsKey(s)) {
            int v = e.status(s) - 1;
            if (v <= 0) e.statuses.remove(s); else e.statuses.put(s, v);
        }
    }

    public void chooseIntents() {
        for (EnemyState e : c.enemies) if (e.alive()) e.intent = EnemyAi.choose(c, e);
    }

    /* ==================== 药水 ==================== */

    public void usePotion(int index) {
        if (index < 0 || index >= c.run.potions.size() || c.over || !c.playerTurn) return;
        PotionState ps = c.run.potions.remove(index);
        PotionDef d = PotionLibrary.get(ps.id);
        if (d == null) return;
        switch (d.kind) {
            case HEAL: {
                int before = c.hp;
                c.hp = Math.min(c.maxHp, c.hp + d.amount);
                c.log("回复 " + (c.hp - before) + " 生命。");
                break;
            }
            case STRENGTH: c.addPlayerStatus(Status.STRENGTH, d.amount); break;
            case DEXTERITY: c.addPlayerStatus(Status.DEXTERITY, d.amount); break;
            case BLOCK: gainBlock(d.amount); break;
            case ENERGY: c.energy += d.amount; break;
            case DRAW: c.draw(d.amount); break;
            case FIRE:
                for (EnemyState e : new ArrayList<>(c.living())) attackEnemy(e, d.amount, 1, false);
                break;
            case WEAK_ALL:
                for (EnemyState e : c.living()) e.addStatus(Status.WEAK, d.amount);
                break;
            case POISON:
                for (EnemyState e : c.living()) e.addStatus(Status.POISON, d.amount);
                break;
            case REGEN: c.addPlayerStatus(Status.REGEN, d.amount); break;
            case THORNS: c.addPlayerStatus(Status.THORNS, d.amount); break;
            case MAX_HP:
                c.maxHp += d.amount;
                c.hp += d.amount;
                break;
            case CLEANSE:
                c.playerStatuses.remove(Status.WEAK);
                c.playerStatuses.remove(Status.VULNERABLE);
                c.playerStatuses.remove(Status.FRAIL);
                c.playerStatuses.remove(Status.POISON);
                c.log("所有减益被洗净。");
                break;
        }
        c.log("使用药水「" + d.name + "」。");
        if (c.living().isEmpty()) endCombat(true);
        else if (c.hp <= 0) endCombat(false);
    }

    /** 托管用：这一回合还能不能打出任何一张牌。 */
    public boolean hasPlayableCard() {
        if (c.over || !c.playerTurn) return false;
        for (CardInstance card : c.hand) if (canPlay(card)) return true;
        return false;
    }

    public void checkAutoWin() {
        if (Cheats.autoWin && !c.over) endCombat(true);
    }

    private void endCombat(boolean win) {
        if (c.over) return;
        c.over = true;
        c.playerTurn = false;
        c.hintUid = "";
        if (win) {
            c.victory = true;
            c.syncRun();
            int heal = c.rPostCombatHeal;
            if (c.rPostCombatHealLow > 0 && c.hp * 2 < c.maxHp) heal += c.rPostCombatHealLow;
            if (heal > 0) {
                int before = c.hp;
                c.hp = Math.min(c.maxHp, c.hp + heal);
                if (c.hp > before) c.log("战后回复 " + (c.hp - before) + " 生命。");
            }
            c.syncRun();
            c.goldReward = c.boss ? 95 + c.rng.nextInt(15) : c.elite ? 30 + c.rng.nextInt(10) : 14 + c.rng.nextInt(9);
            c.log("战斗胜利。");
        } else {
            c.victory = false;
            c.hp = 0;
            if (!c.shadow) {
                c.run.hp = 0;
                c.run.dead = true;
            }
            c.log("墨迹干涸了……");
        }
    }

    public String intentText(EnemyState e) { return EnemyAi.intentText(c, e); }

    public List<String> remainingDeckSummary() {
        ArrayList<String> out = new ArrayList<>();
        for (CardInstance card : c.drawPile) out.add(card.name());
        Collections.sort(out);
        return out;
    }
}
