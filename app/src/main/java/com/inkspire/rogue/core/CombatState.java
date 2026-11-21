package com.inkspire.rogue.core;

import com.inkspire.rogue.ai.EnemyAi;
import com.inkspire.rogue.content.RelicLibrary;
import com.inkspire.rogue.model.CardInstance;
import com.inkspire.rogue.model.CardType;
import com.inkspire.rogue.model.EnemyDef;
import com.inkspire.rogue.model.RelicDef;
import com.inkspire.rogue.model.Status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

public final class CombatState {
    public static final int HAND_LIMIT = 10;

    public final RunState run;
    public final Random rng;
    public final ArrayList<EnemyState> enemies = new ArrayList<>();
    public final ArrayList<CardInstance> drawPile = new ArrayList<>();
    public final ArrayList<CardInstance> hand = new ArrayList<>();
    public final ArrayList<CardInstance> discardPile = new ArrayList<>();
    public final ArrayList<CardInstance> exhaustPile = new ArrayList<>();
    public final ArrayList<CardInstance> powers = new ArrayList<>();
    public final EnumMap<Status, Integer> playerStatuses = new EnumMap<>(Status.class);
    public final ArrayList<String> log = new ArrayList<>();

    public int hp, maxHp, block, energy, maxEnergy, turn;
    public boolean playerTurn = true, over, victory, noDraw;
    public int cardsPlayedThisTurn, attacksPlayed, powersPlayed, cardsPlayedTotal;
    public int penNibCount;
    /** 影子状态标记：AI 推演用的副本，任何写入都不许回落到真实存档。 */
    public boolean shadow;
    public boolean penNibReady;
    public boolean necronomiconReady;
    public int totalDamageDealt, totalDamageTaken;
    public boolean boss, elite;
    public int goldReward;
    public String hintUid = "";
    public String hintText = "";

    /* ---- 遗物在战斗内的聚合值（战斗开始时算一次） ---- */
    public int rEnergyPerTurn, rDrawPerTurn, rBlockPerTurn, rAttackBonus, rCardBlock, rCardPoison;
    public int rStrengthPerTurn, rDexterityPerTurn, rStartDraw, rFirstTurnEnergy, rPostCombatHeal, rPostCombatHealLow;
    public boolean penNibOwned, freeFirstPowerOwned;

    public CombatState(RunState run, List<EnemyState> foes, Random rng) {
        this.run = run;
        this.rng = rng;
        this.enemies.addAll(foes);
        this.hp = run.hp;
        this.maxHp = run.maxHp;
        this.maxEnergy = run.maxEnergy;
        this.energy = maxEnergy;
        for (EnemyState e : enemies) {
            if (e.def.tier == EnemyDef.Tier.BOSS) boss = true;
            if (e.def.tier == EnemyDef.Tier.ELITE) elite = true;
        }
        aggregateRelics();
        for (CardInstance card : run.deck) drawPile.add(card.copy());
        Collections.shuffle(drawPile, rng);
    }

    /** 影子状态专用构造：只挂引用，不发牌、不洗牌。 */
    private CombatState(RunState run, Random rng, boolean bare) {
        this.run = run;
        this.rng = rng;
    }

    /**
     * 复制一份「影子战场」给 AI 推演用：敌人、牌堆、手牌、状态全部独立，
     * 对它的任何操作都不会影响真实战斗、运行进度或存档。
     */
    public CombatState shadow() {
        CombatState s = new CombatState(run, new Random(0x9E3779B97F4A7C15L), true);
        for (EnemyState e : enemies) s.enemies.add(e.copy());
        s.hp = hp;
        s.maxHp = maxHp;
        s.block = block;
        s.energy = energy;
        s.maxEnergy = maxEnergy;
        s.turn = turn;
        s.playerTurn = playerTurn;
        s.over = over;
        s.victory = victory;
        s.noDraw = noDraw;
        s.cardsPlayedThisTurn = cardsPlayedThisTurn;
        s.attacksPlayed = attacksPlayed;
        s.powersPlayed = powersPlayed;
        s.cardsPlayedTotal = cardsPlayedTotal;
        s.penNibCount = penNibCount;
        s.penNibReady = penNibReady;
        s.necronomiconReady = necronomiconReady;
        s.totalDamageDealt = totalDamageDealt;
        s.totalDamageTaken = totalDamageTaken;
        s.boss = boss;
        s.elite = elite;
        s.goldReward = goldReward;
        s.rEnergyPerTurn = rEnergyPerTurn;
        s.rDrawPerTurn = rDrawPerTurn;
        s.rBlockPerTurn = rBlockPerTurn;
        s.rAttackBonus = rAttackBonus;
        s.rCardBlock = rCardBlock;
        s.rCardPoison = rCardPoison;
        s.rStrengthPerTurn = rStrengthPerTurn;
        s.rDexterityPerTurn = rDexterityPerTurn;
        s.rStartDraw = rStartDraw;
        s.rFirstTurnEnergy = rFirstTurnEnergy;
        s.rPostCombatHeal = rPostCombatHeal;
        s.rPostCombatHealLow = rPostCombatHealLow;
        s.penNibOwned = penNibOwned;
        s.freeFirstPowerOwned = freeFirstPowerOwned;
        s.drawPile.addAll(drawPile);
        s.hand.addAll(hand);
        s.discardPile.addAll(discardPile);
        s.exhaustPile.addAll(exhaustPile);
        s.powers.addAll(powers);
        s.playerStatuses.putAll(playerStatuses);
        s.hintUid = hintUid;
        s.hintText = hintText;
        s.shadow = true;
        return s;
    }

    private void aggregateRelics() {
        for (String id : run.relics) {
            RelicDef d = RelicLibrary.get(id);
            if (d == null) continue;
            rEnergyPerTurn += d.energyPerTurn;
            rDrawPerTurn += d.drawPerTurn;
            rBlockPerTurn += d.blockPerTurn;
            rAttackBonus += d.attackBonus;
            rCardBlock += d.cardBlock;
            rCardPoison += d.cardPoison;
            rStrengthPerTurn += d.strengthPerTurn;
            rDexterityPerTurn += d.dexterityPerTurn;
            rStartDraw += d.startDraw;
            rFirstTurnEnergy += d.firstTurnEnergy;
            rPostCombatHeal += d.postCombatHeal;
            rPostCombatHealLow += d.postCombatHealLow;
            if (d.penNib) penNibOwned = true;
            if (d.freeFirstPower) freeFirstPowerOwned = true;
        }
    }

    public int playerStatus(Status s) { Integer v = playerStatuses.get(s); return v == null ? 0 : v; }

    public void addPlayerStatus(Status s, int amount) {
        if (amount == 0) return;
        int now = playerStatus(s) + amount;
        if (now == 0) playerStatuses.remove(s); else playerStatuses.put(s, now);
    }

    public List<EnemyState> living() {
        ArrayList<EnemyState> out = new ArrayList<>();
        for (EnemyState e : enemies) if (e.alive()) out.add(e);
        return out;
    }

    public EnemyState firstLiving() {
        for (EnemyState e : enemies) if (e.alive()) return e;
        return null;
    }

    public void log(String s) {
        if (s == null) return;
        if (!log.isEmpty() && log.get(log.size() - 1).equals(s)) return; // 不刷重复行
        log.add(s);
        if (log.size() > 40) log.remove(0);
    }

    public String lastLog() { return log.isEmpty() ? "" : log.get(log.size() - 1); }

    public int incomingDamage() {
        int sum = 0;
        for (EnemyState e : enemies) {
            if (!e.alive() || e.intent == null) continue;
            sum += EnemyAi.estimateDamage(this, e, e.intent);
        }
        return sum;
    }

    /** 抽牌。中毒/状态牌不会在这里结算，只有「进化」会。 */
    public void draw(int n) {
        draw(n, true);
    }

    private void draw(int n, boolean evolveAllowed) {
        if (noDraw) { log("凝神：本回合无法抽牌。"); return; }
        int evolve = playerStatus(Status.EVOLVE);
        for (int i = 0; i < n; i++) {
            if (hand.size() >= HAND_LIMIT) { log("手牌已满。"); return; }
            if (drawPile.isEmpty()) {
                if (discardPile.isEmpty()) return;
                drawPile.addAll(discardPile);
                discardPile.clear();
                Collections.shuffle(drawPile, rng);
                log("弃牌堆洗回抽牌堆。");
            }
            if (drawPile.isEmpty()) return;
            CardInstance drawn = drawPile.remove(drawPile.size() - 1);
            hand.add(drawn);
            if (evolveAllowed && evolve > 0 && drawn.def != null && drawn.def.type == CardType.STATUS) {
                log("进化：抽到状态牌，再抽 " + evolve + " 张。");
                draw(evolve, false);
            }
        }
    }

    public void syncRun() {
        if (shadow) {
            return;
        }
        run.hp = Math.max(0, Math.min(run.maxHp, hp));
        run.maxHp = maxHp;
    }

    /** 可供 UI 显示的药水栏上限。 */
    public int potionSlots() { return run.potionSlots(); }
}
