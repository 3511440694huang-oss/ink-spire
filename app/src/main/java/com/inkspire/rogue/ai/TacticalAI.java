package com.inkspire.rogue.ai;

import com.inkspire.rogue.content.PotionLibrary;
import com.inkspire.rogue.core.CombatEngine;
import com.inkspire.rogue.core.CombatState;
import com.inkspire.rogue.core.EnemyState;
import com.inkspire.rogue.model.CardInstance;
import com.inkspire.rogue.model.CardType;
import com.inkspire.rogue.model.Effect;
import com.inkspire.rogue.model.PotionDef;
import com.inkspire.rogue.model.Status;
import com.inkspire.rogue.model.Target;

import java.util.ArrayList;
import java.util.List;

/**
 * 「智能推荐」：只读地评估玩家手牌，挑出本回合最值得打出的那一张牌。
 *
 * <p>三条硬性约束：
 * <ul>
 *   <li>只读。不调用任何会改变 {@link CombatState} 的方法。需要借用引擎规则
 *       （{@code canPlay} / {@code currentCost}）时临时创建一个 {@link CombatEngine}，
 *       它的构造函数只是保存引用，没有副作用。</li>
 *   <li>伤害、格挡公式与 {@link CombatEngine} 对齐：力量、虚弱、易伤、多段、双发、笔尖、X 费。</li>
 *   <li>斩杀按「省下这个敌人本回合意图伤害」计价；格挡只买缺口，溢出几乎不值钱；
 *       能力牌按剩余回合摊销，越早越值钱。</li>
 * </ul>
 *
 * <p>评分是启发式的：单位约等于「1 点有效伤害 / 1 点被挡下的伤害」。费用会折算成负分，
 * 所以 0 费的牌天然更灵活；X 费牌按「吞掉全部能量」计算代价。
 */
public final class TacticalAI {

    /** 一点能量大约值多少分，用来把不同费用的牌放在同一把尺子上比较。 */
    private static final double ENERGY_VALUE = 2.6;
    /** 引擎的手牌上限，抽牌不能超过它。 */
    private static final int HAND_LIMIT = 10;
    /** 斩杀的基础分：能杀掉一个敌人本身值这么多。 */
    private static final double KILL_BASE = 42.0;

    private TacticalAI() {
    }

    /** 一条推荐：打哪张牌、打谁、为什么、综合分。 */
    public static final class Suggestion {
        public final CardInstance card;
        public final EnemyState target;
        public final String reason;
        public final int score;

        public Suggestion(CardInstance card, EnemyState target, String reason, int score) {
            this.card = card;
            this.target = target;
            this.reason = reason;
            this.score = score;
        }
    }

    /**
     * 返回本回合最值得打出的牌；没有可出的牌返回 {@code null}。
     * 费用不足、在手上不可打出（诅咒 / 状态 / 引擎拒绝）的牌都会被排除。
     */
    public static Suggestion suggest(CombatState c) {
        if (c == null || c.over || !c.playerTurn || c.hp <= 0) {
            return null;
        }
        if (c.hand.isEmpty()) {
            return null;
        }
        List<EnemyState> alive = c.living();
        if (alive.isEmpty()) {
            return null;
        }

        Ctx ctx = new Ctx(c);
        // 只借规则，不动状态：CombatEngine 的构造函数只保存引用。
        CombatEngine engine = new CombatEngine(c);
        Suggestion best = null;
        int bestSpend = Integer.MAX_VALUE;

        // 先把每种「这张牌打这个目标」的单步评分算出来，作为候选清单。
        ArrayList<Suggestion> cands = new ArrayList<>();
        for (int i = 0; i < c.hand.size(); i++) {
            CardInstance card = c.hand.get(i);
            if (card == null || card.def == null) {
                continue;
            }
            if (!engine.canPlay(card)) {
                continue;
            }
            int cost = engine.currentCost(card); // -1 表示 X 费
            int spend = card.def.xCost ? ctx.energy : Math.max(0, cost);

            if (needsEnemyTarget(card)) {
                // 多个敌人时，每个目标都作为一个候选：三敌局面里"打谁"常常比"打哪张"更关键。
                for (int j = 0; j < alive.size(); j++) {
                    EnemyState target = alive.get(j);
                    Eval ev = evaluate(ctx, engine, card, target, spend);
                    cands.add(new Suggestion(card, target, ev.reason, (int) Math.round(ev.score)));
                }
            } else {
                Eval ev = evaluate(ctx, engine, card, null, spend);
                cands.add(new Suggestion(card, null, ev.reason, (int) Math.round(ev.score)));
            }
        }
        if (cands.isEmpty()) {
            return null;
        }
        // 单步分从高到低；推演只做前若干个，控制单次推荐的开销。
        cands.sort((a, b) -> Integer.compare(b.score, a.score));

        // 关键改进：对候选首步各做一次「把这一回合打完」的影子推演，
        // 用终局价值重排。这样「先上力量/易伤再打」「先抽牌回能」
        // 「先补刀收残血」这些顺序问题会自动浮出来。
        int limit = Math.min(ROLLOUT_LIMIT, cands.size());
        double bestValue = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < limit; i++) {
            Suggestion cand = cands.get(i);
            double value;
            try {
                value = rollout(c, cand.card, cand.target);
            } catch (Throwable err) {
                if (DEBUG_ROLLOUT) { System.out.println("ROLLOUT FAIL " + cand.card.name()); err.printStackTrace(System.out); }
                value = Double.NaN;
            }
            if (Double.isNaN(value)) {
                continue;
            }
            value += cand.score * 0.25; // 同分时偏向单步评分更高的那张
            if (DEBUG_ROLLOUT) System.out.println("  cand " + cand.card.name() + " score=" + cand.score + " value=" + value + " tgt=" + (cand.target==null?"-":cand.target.def.name));
            if (value > bestValue + 1e-6) {
                bestValue = value;
                best = cand;
            }
        }
        // 推演只负责「排序」，不该把负收益的牌推上来：真有更好的正收益选择就用它，
        // 否则托管会拿着一手好牌直接结束回合（报告里抓到过「整回合空过」）。
        if (best != null && best.score <= 0) {
            for (int i = 0; i < cands.size(); i++) {
                if (cands.get(i).score > 0) {
                    return cands.get(i);
                }
            }
        }
        if (best != null) {
            return best;
        }
        // 推演不可用的兜底：直接返回单步最优。
        for (int i = 0; i < cands.size(); i++) {
            Suggestion cand = cands.get(i);
            int spend = cand.card.def.xCost ? ctx.energy : Math.max(0, engine.currentCost(cand.card));
            if (best == null || cand.score > best.score
                    || (cand.score == best.score && spend < bestSpend)) {
                best = cand;
                bestSpend = spend;
            }
        }
        return best;
    }

    /** 调试开关：设置环境变量 MOTA_AI_DEBUG=1 时打印推演细节（正常游戏时永远是 false）。 */
    private static final boolean DEBUG_ROLLOUT = System.getenv("MOTA_AI_DEBUG") != null;

    /** 推演几个候选首步（越多越准，越少越快）。 */
    private static final int ROLLOUT_LIMIT = 24;
    /** 推演里最多再补几张牌。 */
    private static final int ROLLOUT_DEPTH = 12;

    /** 一步推进：打哪张、打谁。 */
    private static final class Step {
        final CardInstance card;
        final EnemyState target;
        Step(CardInstance card, EnemyState target) { this.card = card; this.target = target; }
    }

    /**
     * 影子推演：把候选首步打出去，之后用一个便宜的贪心把本回合能打的都打完，
     * 再给终局打分。影子状态与真实战斗完全隔离（见 CombatState.shadow）。
     */
    private static double rollout(CombatState base, CardInstance first, EnemyState target) {
        CombatState s = base.shadow();
        CombatEngine engine = new CombatEngine(s);
        EnemyState tg = target == null ? null : ghostOf(base, s, target);
        engine.play(first, tg);
        int guard = 0;
        while (!s.over && s.playerTurn && guard++ < ROLLOUT_DEPTH) {
            Step step = greedyStep(s);
            if (step == null) {
                break;
            }
            engine.play(step.card, step.target);
        }
        return boardValue(base, s);
    }

    /** 影子里的同一只敌人（按顺序对应）。 */
    private static EnemyState ghostOf(CombatState base, CombatState shadow, EnemyState e) {
        int idx = base.enemies.indexOf(e);
        if (idx >= 0 && idx < shadow.enemies.size()) {
            return shadow.enemies.get(idx);
        }
        return shadow.firstLiving();
    }

    /** 推演内部的便宜策略：只按单步评分挑一张正收益的牌。 */
    private static Step greedyStep(CombatState s) {
        Ctx ctx = new Ctx(s);
        CombatEngine engine = new CombatEngine(s);
        Step best = null;
        double bestScore = 0.6;
        for (int i = 0; i < s.hand.size(); i++) {
            CardInstance card = s.hand.get(i);
            if (card == null || card.def == null || !engine.canPlay(card)) {
                continue;
            }
            int spend = card.def.xCost ? ctx.energy : Math.max(0, engine.currentCost(card));
            if (needsEnemyTarget(card)) {
                for (int j = 0; j < ctx.alive.size(); j++) {
                    Eval ev = evaluate(ctx, engine, card, ctx.alive.get(j), spend);
                    if (ev.score > bestScore) {
                        bestScore = ev.score;
                        best = new Step(card, ctx.alive.get(j));
                    }
                }
            } else {
                Eval ev = evaluate(ctx, engine, card, null, spend);
                if (ev.score > bestScore) {
                    bestScore = ev.score;
                    best = new Step(card, null);
                }
            }
        }
        return best;
    }

    /**
     * 终局打分：伤害 + 击杀（含省下的意图伤害）+ 有效格挡 + 生命 + 状态资产。
     * 生命按 2 倍计——它跨战斗累积，比单回合的伤害更贵。
     */
    private static double boardValue(CombatState base, CombatState end) {
        double v = 0;
        int threat = base.incomingDamage();
        int n = Math.min(base.enemies.size(), end.enemies.size());
        for (int i = 0; i < n; i++) {
            EnemyState a = base.enemies.get(i);
            EnemyState b = end.enemies.get(i);
            v += Math.max(0, a.hp - Math.max(0, b.hp));
            v += Math.max(0, a.block - Math.max(0, b.block)) * 0.35;
            if (a.alive() && !b.alive()) {
                v += 60 + EnemyAi.estimateDamage(base, a, a.intent) * 1.4;
            }
            v += (b.status(Status.VULNERABLE) - a.status(Status.VULNERABLE)) * 2.2;
            v += (b.status(Status.WEAK) - a.status(Status.WEAK)) * 2.2;
            v += (b.status(Status.POISON) - a.status(Status.POISON)) * 1.6;
        }
        int gained = Math.max(0, end.block - base.block);
        v += Math.min(gained, Math.max(0, threat - base.block)) * 1.1;
        v += (end.hp - base.hp) * 2.0;
        v += (end.maxHp - base.maxHp) * 1.4;
        v += (end.playerStatus(Status.STRENGTH) - base.playerStatus(Status.STRENGTH)) * 3.0;
        v += (end.playerStatus(Status.DEXTERITY) - base.playerStatus(Status.DEXTERITY)) * 2.0;
        v += (end.playerStatus(Status.THORNS) - base.playerStatus(Status.THORNS)) * 1.5;
        v += (end.playerStatus(Status.METALLICIZE) - base.playerStatus(Status.METALLICIZE)) * 1.5;
        v += (end.playerStatus(Status.REGEN) - base.playerStatus(Status.REGEN)) * 1.2;
        v += (end.playerStatus(Status.BARRICADE) - base.playerStatus(Status.BARRICADE)) * 9.0;
        v -= (end.playerStatus(Status.WEAK) - base.playerStatus(Status.WEAK)) * 2.0;
        v -= (end.playerStatus(Status.VULNERABLE) - base.playerStatus(Status.VULNERABLE)) * 2.5;
        v -= (end.playerStatus(Status.POISON) - base.playerStatus(Status.POISON)) * 2.0;
        if (end.over && end.victory && !base.over) {
            v += 500; // 本回合直接清场
        }
        return v;
    }

    /**
     * 托管一步：拿到推荐后真正打出去，返回这一步是否产生了动作。
     * 战斗结束、非玩家回合、没有可出的牌时返回 {@code false}。
     */
    public static boolean autoplayOne(CombatEngine engine) {
        if (engine == null) {
            return false;
        }
        CombatState c = engine.c;
        if (c == null || c.over || !c.playerTurn || c.hp <= 0) {
            return false;
        }
        Suggestion s = suggest(c);
        if (s == null || s.card == null) {
            return false;
        }
        // 综合收益为负的牌一律不打：0 费也一样（报告里 0 费负收益牌照打会白掉血）
        if (s.score <= 0 && !s.card.def.xCost) {
            return false;
        }
        EnemyState target = s.target;
        if (target != null && !target.alive()) {
            target = c.firstLiving();
        }
        int playedBefore = c.cardsPlayedTotal;
        int handBefore = c.hand.size();
        engine.play(s.card, target);
        return c.cardsPlayedTotal > playedBefore || c.hand.size() < handBefore;
    }

    /**
     * 托管用药水：只在「能救命 / 能清场 / 能斩杀」时才建议。返回 -1 表示别喝。
     * 这是保守策略——药水是稀缺资源，托管不应该随手倒掉。
     */
    public static int bestPotionIndex(CombatState c) {
        if (c == null || c.over || !c.playerTurn || c.run == null || c.run.potions.isEmpty()) {
            return -1;
        }
        int incoming = c.incomingDamage();
        int block = Math.max(0, c.block);
        int missingBlock = Math.max(0, incoming - block);
        boolean lethal = missingBlock > 0 && missingBlock >= c.hp;
        // 手里还能补多少伤害：药水 + 一张最大的攻击，够了就能收掉一只
        int handHit = maxHandHit(c);
        int livingHp = 0;
        for (int k = 0; k < c.enemies.size(); k++) {
            EnemyState e = c.enemies.get(k);
            if (e.alive()) livingHp += e.hp + Math.max(0, e.block);
        }
        int best = -1;
        double bestScore = 34.0;
        for (int i = 0; i < c.run.potions.size(); i++) {
            PotionDef d = PotionLibrary.get(c.run.potions.get(i).id);
            if (d == null) {
                continue;
            }
            double score = 0;
            switch (d.kind) {
                case BLOCK:
                    if (lethal) score = 100 + d.amount;
                    else if (missingBlock > 0 && missingBlock >= c.maxHp * 0.3) score = 40 + d.amount;
                    break;
                case HEAL:
                    if (lethal) score = 95 + d.amount;
                    else if (c.hp * 100 <= c.maxHp * 50) score = 50 + d.amount;
                    break;
                case FIRE: {
                    // 关键：不是"药水自己能不能杀"，而是"药水 + 手里这一刀能不能杀"，
                    // 以及群伤药水对全场打掉多少血。
                    int kills = 0;
                    int dealt = 0;
                    for (int k = 0; k < c.enemies.size(); k++) {
                        EnemyState e = c.enemies.get(k);
                        if (!e.alive()) continue;
                        int effective = e.hp + Math.max(0, e.block);
                        dealt += Math.min(d.amount, effective);
                        if (effective <= d.amount + handHit) kills++;
                    }
                    if (kills > 0) {
                        score = 88 + kills * 40 + dealt * 0.5;
                        if (lethal) score += 20;
                        if (kills >= c.living().size()) score += 40; // 直接清场
                    } else if (c.living().size() >= 2 && dealt >= 26) {
                        score = 44 + dealt * 0.3; // 群伤药水，两个以上目标时够本
                    }
                    break;
                }
                case STRENGTH:
                    if (c.turn <= 2 && livingHp >= 60) score = 40 + d.amount * 3;
                    break;
                case DEXTERITY:
                    if (c.turn <= 2 && incoming >= 8) score = 38 + d.amount * 2;
                    break;
                case ENERGY:
                    if (c.energy <= 0 && !c.hand.isEmpty()) score = 42;
                    break;
                case DRAW:
                    // 抽了还得有能量打，否则等于空转
                    if (c.hand.size() <= 3 && c.energy >= 2) score = 36 + d.amount;
                    break;
                case CLEANSE:
                    if (c.playerStatus(Status.WEAK) + c.playerStatus(Status.VULNERABLE) >= 3) score = 40;
                    break;
                case POISON: {
                    if (c.living().size() >= 2 && livingHp > 80) score = 38;
                    break;
                }
                default:
                    break;
            }
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    /** 手牌里单张攻击最多能打多少（给"药水 + 一刀 = 斩杀"用）。 */
    private static int maxHandHit(CombatState c) {
        int strength = c.playerStatus(Status.STRENGTH) + c.rAttackBonus;
        boolean weak = c.playerStatus(Status.WEAK) > 0;
        int best = 0;
        for (int i = 0; i < c.hand.size(); i++) {
            CardInstance card = c.hand.get(i);
            if (card.def == null) continue;
            List<Effect> effects = card.effects();
            for (int k = 0; k < effects.size(); k++) {
                Effect e = effects.get(k);
                if (e.kind != Effect.Kind.DAMAGE && e.kind != Effect.Kind.DAMAGE_RANDOM) continue;
                int one = estimateHit(e.amount, e.strengthMultiplier, strength, weak);
                int total = one * Math.max(1, e.hits);
                if (total > best) best = total;
            }
        }
        return best;
    }

    /** 给 UI 的一行解释，例如「斩杀：可击杀 墨蛭，避免 12 点伤害」。 */
    public static String shortReason(Suggestion s) {
        if (s == null || s.card == null) {
            return "没有可打出的牌。";
        }
        String text = s.reason;
        if (text == null) {
            text = "";
        }
        text = text.trim();
        int cut = text.indexOf('\n');
        if (cut >= 0) {
            text = text.substring(0, cut);
        }
        if (text.isEmpty()) {
            text = "建议打出「" + s.card.name() + "」";
            if (s.target != null) {
                text = text + "，目标 " + s.target.def.name;
            }
        }
        return text;
    }

    // ------------------------------------------------------------------
    // 战场快照：一次 suggest 只算一次，后面所有评分共用
    // ------------------------------------------------------------------

    private static final class Ctx {
        final CombatState c;
        final List<EnemyState> alive;
        final int incoming;
        final int block;
        final int hp;
        final int maxHp;
        final int energy;
        final int handSize;
        final int missingBlock;
        final int drawPool;
        final int strength;
        final int dexterity;
        final int attacksInHand;
        final int attackHits;
        final int attackPotential;
        final int attackBonus;
        final int estTurns;
        final int totalEnemyHp;
        final boolean lethal;
        final boolean lowHp;
        final boolean weak;
        final boolean frail;
        final boolean barricade;
        final boolean penNib;
        final int doubleTap;

        Ctx(CombatState c) {
            this.c = c;
            this.alive = c.living();
            this.incoming = c.incomingDamage();
            this.block = Math.max(0, c.block);
            this.hp = Math.max(0, c.hp);
            this.maxHp = Math.max(1, c.maxHp);
            this.energy = Math.max(0, c.energy);
            this.handSize = c.hand.size();
            this.missingBlock = Math.max(0, incoming - block);
            this.lethal = incoming > 0 && missingBlock >= hp;
            this.lowHp = hp * 100 <= maxHp * 35;
            this.drawPool = c.drawPile.size() + c.discardPile.size();
            this.strength = c.playerStatus(Status.STRENGTH);
            this.dexterity = c.playerStatus(Status.DEXTERITY);
            this.weak = c.playerStatus(Status.WEAK) > 0;
            this.frail = c.playerStatus(Status.FRAIL) > 0;
            this.barricade = c.playerStatus(Status.BARRICADE) > 0;
            this.penNib = c.penNibReady;
            this.doubleTap = c.playerStatus(Status.DOUBLE_TAP);
            this.attacksInHand = countAttacks(c);
            this.attackHits = countEnemyHits(c);
            this.attackPotential = estimateAttackPotential(c);
            this.attackBonus = c.rAttackBonus;
            this.totalEnemyHp = sumHp(alive);
            // 每回合真实输出大概是「能量 × 7 + 力量加成」，别再按手牌攻击数虚高
            int perTurn = Math.max(7, energy * 7 + Math.max(0, strength) * 2);
            int turns = (int) Math.ceil(totalEnemyHp / (double) Math.max(1, perTurn));
            this.estTurns = Math.max(1, Math.min(8, turns));
        }
    }

    /** 评分时用的敌人影子，扣血 / 破盾都在这里推演，不碰真实敌人。 */
    private static final class Foe {
        final EnemyState e;
        int hp;
        int block;
        boolean killed;

        Foe(EnemyState e) {
            this.e = e;
            this.hp = e.hp;
            this.block = e.block;
        }
    }

    /** 一张牌在某个目标上的评分明细。 */
    private static final class Eval {
        final Ctx ctx;
        final CardInstance card;
        final EnemyState target;

        double score;
        double offense;
        double defense;
        double status;
        double power;
        double utility;
        double targetBonus;

        int damage;
        int kills;
        int killIntentSaved;
        int blockBroken;
        int rawBlock;
        int draw;
        int energyGain;
        int heal;
        int maxHpGain;
        int selfDamage;

        double bestStatusValue;
        Status mainStatus;
        int mainStatusAmount;
        Status mainPower;
        int mainPowerAmount;

        boolean lethalSave;
        String reason = "";

        Eval(Ctx ctx, CardInstance card, EnemyState target) {
            this.ctx = ctx;
            this.card = card;
            this.target = target;
        }
    }

    // ------------------------------------------------------------------
    // 核心：一张牌打在一个目标上的评分
    // ------------------------------------------------------------------

    private static Eval evaluate(Ctx ctx, CombatEngine engine, CardInstance card,
                                 EnemyState selected, int spend) {
        final List<Effect> effects = card.effects();
        Eval ev = new Eval(ctx, card, selected);
        ArrayList<Foe> foes = snapshot(ctx.alive);
        int simBlock = ctx.block;

        boolean attackCard = card.def.type == CardType.ATTACK;
        int repeats = attackCard && ctx.doubleTap > 0 ? 2 : 1;
        if (card.def.type == CardType.CURSE || card.def.type == CardType.STATUS) {
            repeats = 0;
        }

        for (int r = 0; r < repeats; r++) {
            for (int k = 0; k < effects.size(); k++) {
                Effect e = effects.get(k);
                switch (e.kind) {
                    case DAMAGE: {
                        Foe f = foeOf(foes, selected);
                        if (f == null || f.hp <= 0) {
                            break;
                        }
                        int dealt = applyHits(ctx, e, f, e.hits, ev);
                        ev.damage += dealt;
                        ev.offense += dealt;
                        break;
                    }
                    case DAMAGE_ALL: {
                        int total = 0;
                        for (int i = 0; i < foes.size(); i++) {
                            Foe f = foes.get(i);
                            if (f.hp <= 0) {
                                continue;
                            }
                            total += applyHits(ctx, e, f, e.hits, ev);
                        }
                        ev.damage += total;
                        ev.offense += total;
                        // 多敌人时 AOE 额外加权：一次结算压住全场，价值随人数上升
                        if (foes.size() > 1 && total > 0) {
                            ev.offense += total * 0.06 * (foes.size() - 1);
                        }
                        break;
                    }
                    case DAMAGE_RANDOM: {
                        if (foes.isEmpty()) {
                            break;
                        }
                        if (foes.size() == 1) {
                            Foe only = foes.get(0);
                            if (only.hp > 0) {
                                int dealt = applyHits(ctx, e, only, e.hits, ev);
                                ev.damage += dealt;
                                ev.offense += dealt;
                            }
                        } else {
                            // 多个目标时只算期望：随机打散，斩杀不可预期，折价 15%
                            double expected = 0;
                            for (int i = 0; i < foes.size(); i++) {
                                Foe f = foes.get(i);
                                if (f.hp <= 0) {
                                    continue;
                                }
                                int one = hitDamage(ctx, e.amount, e.strengthMultiplier, f.e);
                                double share = (double) e.hits / foes.size();
                                double loss = Math.min(share * one, f.hp + f.block);
                                expected += loss * 0.85;
                            }
                            ev.damage += (int) Math.round(expected);
                            ev.offense += expected;
                        }
                        break;
                    }
                    case DAMAGE_X: {
                        // X 费 = 当前全部能量，一次抽干
                        int x = ctx.energy;
                        if (x <= 0) {
                            break;
                        }
                        int total = 0;
                        for (int i = 0; i < foes.size(); i++) {
                            Foe f = foes.get(i);
                            if (f.hp <= 0) {
                                continue;
                            }
                            total += applyHits(ctx, e, f, x * e.hits, ev);
                        }
                        ev.damage += total;
                        ev.offense += total;
                        break;
                    }
                    case DAMAGE_BLOCK: {
                        Foe f = foeOf(foes, selected);
                        if (f == null || f.hp <= 0) {
                            break;
                        }
                        // 全身撞击：伤害 = 当前格挡，力量照常吃
                        int dealt = applyRawDamage(f, hitDamage(ctx, simBlock, 1, f.e), ev);
                        ev.damage += dealt;
                        ev.offense += dealt;
                        break;
                    }
                    case BLOCK: {
                        int gain = e.amount + Math.max(0, ctx.dexterity);
                        if (ctx.frail) {
                            gain = (int) Math.floor(gain * 0.75);
                        }
                        gain = Math.max(0, gain);
                        if (gain <= 0) {
                            break;
                        }
                        simBlock += gain;
                        ev.rawBlock += gain;
                        break;
                    }
                    case DRAW: {
                        if (ctx.c.noDraw) {
                            break;
                        }
                        int slots = Math.max(0, HAND_LIMIT - (ctx.handSize - 1));
                        int n = Math.min(e.amount, Math.min(slots, ctx.drawPool));
                        if (n > 0) {
                            ev.draw += n;
                        }
                        break;
                    }
                    case ENERGY: {
                        if (e.amount > 0) {
                            ev.energyGain += e.amount;
                        }
                        break;
                    }
                    case HEAL: {
                        int missing = Math.max(0, ctx.maxHp - ctx.hp);
                        ev.heal += Math.min(e.amount, missing);
                        break;
                    }
                    case MAX_HP: {
                        if (e.amount > 0) {
                            ev.maxHpGain += e.amount;
                        }
                        break;
                    }
                    case LOSE_HP: {
                        ev.selfDamage += Math.max(0, e.amount);
                        break;
                    }
                    case APPLY_ENEMY: {
                        Foe f = foeOf(foes, selected);
                        if (f == null || f.hp <= 0) {
                            break;
                        }
                        double v = statusValue(ctx, e.status, e.amount, f.e);
                        if (v != 0) {
                            ev.status += v;
                            if (v > ev.bestStatusValue) {
                                ev.bestStatusValue = v;
                                ev.mainStatus = e.status;
                                ev.mainStatusAmount = e.amount;
                            }
                        }
                        break;
                    }
                    case APPLY_ALL: {
                        for (int i = 0; i < foes.size(); i++) {
                            Foe f = foes.get(i);
                            if (f.hp <= 0) {
                                continue;
                            }
                            double v = statusValue(ctx, e.status, e.amount, f.e);
                            if (v == 0) {
                                continue;
                            }
                            ev.status += v;
                            if (v > ev.bestStatusValue) {
                                ev.bestStatusValue = v;
                                ev.mainStatus = e.status;
                                ev.mainStatusAmount = e.amount;
                            }
                        }
                        break;
                    }
                    case POWER: {
                        double v = powerValue(ctx, e.status, e.amount);
                        if (v != 0) {
                            ev.power += v;
                            if (ev.mainPower == null) {
                                ev.mainPower = e.status;
                                ev.mainPowerAmount = e.amount;
                            }
                        }
                        break;
                    }
                    case DOUBLE_STRENGTH: {
                        int str = Math.max(0, ctx.c.playerStatus(Status.STRENGTH));
                        if (str > 0) {
                            ev.power += str * 4.5;
                            if (ev.mainPower == null) {
                                ev.mainPower = Status.STRENGTH;
                                ev.mainPowerAmount = str;
                            }
                        }
                        break;
                    }
                    case DOUBLE_BLOCK: {
                        int before = simBlock;
                        simBlock *= 2;
                        int gain = simBlock - before;
                        if (gain > 0) {
                            ev.rawBlock += gain;
                        }
                        break;
                    }
                    case EXHAUST_RANDOM: {
                        int n = Math.min(e.amount, Math.max(0, ctx.handSize - 1));
                        if (n <= 0) {
                            break;
                        }
                        ev.utility -= n * 1.5;
                        if (ctx.handSize - 1 <= n) {
                            ev.utility -= 6; // 把整只手烧光，代价很大
                        }
                        int fnp = ctx.c.playerStatus(Status.FEEL_NO_PAIN);
                        if (fnp > 0) {
                            ev.rawBlock += n * fnp;
                        }
                        if (ctx.c.playerStatus(Status.DARK_EMBRACE) > 0) {
                            ev.draw += n;
                        }
                        break;
                    }
                    case EXHAUST_NON_ATTACK_BLOCK: {
                        int count = 0;
                        for (int i = 0; i < ctx.c.hand.size(); i++) {
                            CardInstance h = ctx.c.hand.get(i);
                            if (h == card || h.def == null) {
                                continue;
                            }
                            if (h.def.type != CardType.ATTACK) {
                                count++;
                            }
                        }
                        if (count <= 0) {
                            break;
                        }
                        int gain = count * e.amount;
                        int fnp = Math.max(0, ctx.c.playerStatus(Status.FEEL_NO_PAIN));
                        simBlock += gain + count * fnp;
                        ev.rawBlock += gain + count * fnp;
                        if (ctx.c.playerStatus(Status.DARK_EMBRACE) > 0) {
                            ev.draw += count;
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
            if (allDead(foes)) {
                break;
            }
        }

        // 虚无牌：这回合不打就永久消失，给它一点紧迫感
        if (card.def.ethereal) {
            ev.utility += 4.0;
        }
        // 一次清场：直接结束这场战斗，远超任何数值收益
        if (allDead(foes)) {
            ev.offense += 260;
        }

        // 本牌自身的消耗 / 击杀特效（引擎在结算完之后处理）
        if (card.exhaust()) {
            int fnp = Math.max(0, ctx.c.playerStatus(Status.FEEL_NO_PAIN));
            if (fnp > 0) {
                ev.rawBlock += fnp;
            }
            if (ctx.c.playerStatus(Status.DARK_EMBRACE) > 0) {
                ev.draw += 1;
            }
        }
        if (card.def.healUnblocked && ev.damage > 0) {
            int missing = Math.max(0, ctx.maxHp - ctx.hp);
            ev.heal += Math.min(ev.damage, missing);
        }
        if (card.def.onKillMaxHp > 0 && ev.kills > 0) {
            ev.maxHpGain += card.def.onKillMaxHp * ev.kills;
        }
        if (card.def.xCost && ctx.energy <= 0) {
            ev.utility -= 5; // 没能量时 X 费牌等于空过
        }
        if (card.noDraw()) {
            ev.utility -= otherDrawCards(ctx, card) * 2.0;
        }

        // ---- 格挡只买缺口：算完斩杀后重新评估还剩多少威胁 ----
        int pending = pendingIncoming(ctx.c, foes);
        int need = Math.max(0, pending - ctx.block);
        int effective = Math.min(ev.rawBlock, need);
        int overflow = Math.max(0, ev.rawBlock - effective);
        int took = Math.max(0, pending - ctx.block - effective);
        boolean wasDead = pending > 0 && pending - ctx.block >= ctx.hp;
        ev.lethalSave = wasDead && took < ctx.hp;

        // 生命是整局共用的稀缺资源：挡住 1 点伤害比打出 1 点伤害更值钱。
        // 但格挡不能跨回合结转，所以只按「实际挡下的缺口」计价，溢出几乎不值钱。
        double blockCoef = ev.lethalSave ? 2.4 : (pending > 0 ? 1.45 : 0.16);
        if (ctx.lowHp && pending > 0) {
            blockCoef += 0.35;
        }
        if (ctx.barricade) {
            blockCoef += 0.25; // 壁垒下格挡能带到下一回合
        }
        ev.defense = effective * blockCoef
                + overflow * (ctx.barricade ? 0.5 : 0.08) * (wasDead ? 1.5 : 1.0);

        // ---- 抽牌：手牌越少越值钱，没能量时打折 ----
        if (ev.draw > 0) {
            double unit = 2.3;
            int handAfter = Math.max(0, ctx.handSize - 1);
            if (handAfter <= 2) {
                unit = 4.1;
            } else if (handAfter <= 4) {
                unit = 2.9;
            }
            if (ctx.energy - spend <= 0) {
                unit *= 0.5;
            }
            ev.utility += ev.draw * unit;
        }

        // ---- 能量：只有还能继续出牌才值钱 ----
        if (ev.energyGain > 0) {
            int otherCost = otherPlayableCost(ctx, engine, card);
            int gap = Math.max(0, otherCost - Math.max(0, ctx.energy - spend));
            double used = Math.min(ev.energyGain, gap);
            ev.utility += used * ENERGY_VALUE + (ev.energyGain - used) * 0.35;
        }

        if (ev.heal > 0) {
            ev.utility += ev.heal * (ctx.lowHp ? 1.5 : 0.8);
        }
        if (ev.maxHpGain > 0) {
            ev.utility += ev.maxHpGain * 1.3;
        }
        if (ev.selfDamage > 0) {
            ev.utility -= ev.selfDamage * (ctx.lowHp ? 1.8 : 0.85);
            if (ev.selfDamage >= ctx.hp) {
                ev.utility -= 500; // 自杀式打牌直接劝退
            }
        }
        if (ev.blockBroken > 0) {
            ev.offense += ev.blockBroken * 0.45; // 破盾是给后续攻击铺路，只算半价
        }
        if (selected != null) {
            ev.targetBonus = targetPreference(ctx, card, selected);
        }
        if (wasDead) {
            ev.power *= 0.45; // 要死了就别再投资成长牌
        } else if (ctx.hp < ctx.incoming * 2.2 && ctx.estTurns > 2) {
            ev.power *= 0.7; // 血不富裕：先保命，别急着铺长线
        }
        if (ctx.estTurns <= 1) {
            ev.power *= 0.35; // 这一回合就能收尾，成长牌来不及兑现
        }

        ev.score = ev.offense + ev.defense + ev.status + ev.power + ev.utility
                + ev.targetBonus - spend * ENERGY_VALUE;
        ev.reason = buildReason(ctx, ev, pending, took);
        return ev;
    }

    // ------------------------------------------------------------------
    // 伤害推演（与 CombatEngine.attackEnemy 的公式一致）
    // ------------------------------------------------------------------

    private static int hitDamage(Ctx ctx, int base, int strMultiplier, EnemyState target) {
        int dmg = base + ctx.strength * Math.max(1, strMultiplier) + ctx.attackBonus;
        if (ctx.weak) {
            dmg = (int) Math.floor(dmg * 0.75);
        }
        if (target != null && target.status(Status.VULNERABLE) > 0) {
            dmg = (int) Math.floor(dmg * 1.5);
        }
        if (ctx.penNib) {
            dmg *= 2;
        }
        return Math.max(0, dmg);
    }

    private static int applyHits(Ctx ctx, Effect e, Foe f, int hits, Eval ev) {
        int total = 0;
        for (int i = 0; i < hits; i++) {
            if (f.hp <= 0) {
                break;
            }
            total += applyRawDamage(f, hitDamage(ctx, e.amount, e.strengthMultiplier, f.e), ev);
        }
        return total;
    }

    private static int applyRawDamage(Foe f, int dmg, Eval ev) {
        if (f == null || f.hp <= 0 || dmg <= 0) {
            return 0;
        }
        int absorbed = Math.min(f.block, dmg);
        f.block -= absorbed;
        ev.blockBroken += absorbed;
        int loss = dmg - absorbed;
        int real = Math.min(loss, f.hp);
        f.hp -= real;
        if (f.hp <= 0 && !f.killed) {
            f.killed = true;
            ev.kills++;
            int intent = EnemyAi.estimateDamage(ev.ctx.c, f.e, f.e.intent);
            ev.killIntentSaved += intent;
            ev.offense += killBonus(ev.ctx, f.e, intent);
        }
        return real;
    }

    private static double killBonus(Ctx ctx, EnemyState e, int intentDamage) {
        double bonus = KILL_BASE + intentDamage * 1.2;
        if (ctx.alive.size() == 1) {
            bonus += 140; // 最后一个敌人：直接结束战斗
        }
        if (ctx.lethal) {
            bonus *= 1.3; // 危急时刻，杀掉威胁就是保命
        }
        return bonus;
    }

    // ------------------------------------------------------------------
    // 状态与能力估值
    // ------------------------------------------------------------------

    private static double statusValue(Ctx ctx, Status s, int amount, EnemyState e) {
        if (s == null || amount == 0) {
            return 0;
        }
        if (s == Status.VULNERABLE && amount > 0) {
            int existing = Math.max(0, e.status(Status.VULNERABLE));
            double per;
            if (ctx.attacksInHand <= 0) {
                per = 1.1; // 本回合没攻击牌，只剩跨回合价值
            } else {
                per = 0.5 * avgAttackDamage(ctx) * 0.8;
                if (existing >= 3) {
                    per *= 0.35;
                } else if (existing >= 1) {
                    per *= 0.7;
                }
            }
            return per * amount;
        }
        if (s == Status.WEAK && amount > 0) {
            int inc = EnemyAi.estimateDamage(ctx.c, e, e.intent);
            double per = inc > 0 ? 0.25 * inc * 1.25 : 0.9;
            if (e.status(Status.WEAK) >= 2) {
                per *= 0.4;
            }
            return per * amount;
        }
        if (s == Status.FRAIL && amount > 0) {
            int enemyBlock = EnemyAi.estimateBlock(ctx.c, e, e.intent);
            double per = enemyBlock > 0 ? 0.35 * enemyBlock : 0.25;
            return per * amount;
        }
        if (s == Status.POISON && amount > 0) {
            // 每层每回合掉 1 点血、掉完减 1 层：n 层在 t 回合内的总伤害 = 递减求和
            int turns = Math.max(1, Math.min(ctx.estTurns, amount));
            double total = turns * amount - (double) turns * (turns - 1) / 2.0;
            // 毒会被敌人的格挡减免吗？不会，但来得慢，折价
            return total * 0.8;
        }
        if (s == Status.SHACKLE && amount > 0) {
            int hits = hitsInIntent(e);
            int inc = EnemyAi.estimateDamage(ctx.c, e, e.intent);
            double now = inc > 0 && hits > 0 ? Math.min(amount, Math.max(1, inc / Math.max(1, hits))) * hits : 0;
            return now * 1.1;
        }
        if (s == Status.STRENGTH && amount < 0) {
            int loss = -amount;
            int hits = hitsInIntent(e);
            int inc = EnemyAi.estimateDamage(ctx.c, e, e.intent);
            double now = inc > 0 ? Math.min(loss, Math.max(1, inc / Math.max(1, hits))) * hits * 1.1 : 0;
            double future = Math.min(loss, 6) * Math.min(4, ctx.estTurns) * 0.9;
            return now + future;
        }
        return Math.abs(amount) * 0.5;
    }

    private static double powerValue(Ctx ctx, Status s, int amount) {
        if (s == null || amount == 0) {
            return 0;
        }
        int turns = Math.min(5, ctx.estTurns);
        switch (s) {
            case STRENGTH:
                return amount * 1.7 * turns;
            case DEXTERITY:
                return amount * 1.0 * turns;
            case DEMON_FORM:
                return Math.min(48, amount * 1.9 * turns * (1.0 + turns * 0.25));
            case METALLICIZE:
                return amount * 0.85 * turns;
            case THORNS:
                // 敌人这回合不打人时，棘刺本回合一分不值（报告里被抓过：0 伤意图还先上荆甲）
                if (ctx.attackHits <= 0) {
                    return amount * 0.25 * Math.min(2, ctx.estTurns);
                }
                return amount * ctx.attackHits * 0.55 * Math.min(3, ctx.estTurns);
            case JUGGERNAUT:
                return amount * 0.9 * turns;
            case BARRICADE:
                return 9 + 1.6 * turns;
            case CORRUPTION:
                return 10 + 2.2 * countSkills(ctx.c);
            case BERSERK:
                return amount * ENERGY_VALUE * turns * 0.9;
            case BRUTALITY:
                return amount * 1.1 * turns;
            case REGEN:
                return amount * 0.8 * turns;
            case FEEL_NO_PAIN:
                return amount * 0.9 * Math.min(4, ctx.estTurns);
            case DARK_EMBRACE:
                return 1.6 * Math.min(4, ctx.estTurns);
            case RUPTURE:
                return 3 + amount * 1.5 * Math.min(4, ctx.estTurns);
            case DOUBLE_TAP:
                return 0.9 * bestAttackDamage(ctx);
            case FLAME_BARRIER:
                return amount * Math.max(1, ctx.attackHits) * 0.6;
            case NO_DRAW:
                return -3;
            default:
                return amount * 0.5;
        }
    }

    // ------------------------------------------------------------------
    // 目标选择：斩杀 > 高意图 > 未被削弱且血厚的
    // ------------------------------------------------------------------

    private static double targetPreference(Ctx ctx, CardInstance card, EnemyState t) {
        int inc = EnemyAi.estimateDamage(ctx.c, t, t.intent);
        double v = inc * 0.55; // 优先处理这回合打你最疼的那只
        if (dealsDamage(card)) {
            // 补刀优先：伤害摊在几只身上会导致一只都杀不掉，等于白打。
            // 先把「这一刀就能收掉」的、再按残血程度排序，快速减少出手的敌人数量。
            int effective = t.hp + Math.max(0, t.block);
            v += Math.max(0, 30 - effective) * 0.9;
            v += (1.0 - t.hp / (double) Math.max(1, t.maxHp)) * 8.0;
            if (effective <= 8) {
                v += 10.0;
            }
        } else {
            if (appliesStatus(card, Status.VULNERABLE)) {
                v += t.status(Status.VULNERABLE) == 0 ? 6.0 : -2.0 * t.status(Status.VULNERABLE);
            }
            if (appliesStatus(card, Status.WEAK)) {
                v += t.status(Status.WEAK) == 0 ? 5.0 : -1.5 * t.status(Status.WEAK);
            }
            if (appliesNegativeStrength(card)) {
                v += Math.max(0, t.status(Status.STRENGTH)) * 1.2;
            }
            v += (t.hp / (double) Math.max(1, t.maxHp)) * 4.0;
        }
        return v;
    }

    // ------------------------------------------------------------------
    // 理由文本
    // ------------------------------------------------------------------

    private static String buildReason(Ctx ctx, Eval ev, int pending, int took) {
        String who = ev.target == null ? "全体敌人" : ev.target.def.name;
        if (ev.kills > 1) {
            return "清场：一次击杀 " + ev.kills + " 个敌人";
        }
        if (ev.kills == 1) {
            if (ev.killIntentSaved > 0) {
                return "斩杀：可击杀 " + who + "，避免 " + ev.killIntentSaved + " 点伤害";
            }
            return "斩杀：可击杀 " + who;
        }
        if (ev.lethalSave) {
            return "保命：补 " + ev.rawBlock + " 点格挡，把本回合伤害压到 " + took + " 点";
        }

        int tag = 0;
        double best = ev.offense;
        if (ev.defense > best) {
            best = ev.defense;
            tag = 1;
        }
        if (ev.status > best) {
            best = ev.status;
            tag = 2;
        }
        if (ev.power > best) {
            best = ev.power;
            tag = 3;
        }
        if (ev.utility > best) {
            best = ev.utility;
            tag = 4;
        }
        if (best <= 0.5) {
            tag = 5;
        }

        switch (tag) {
            case 0: {
                if (ev.damage > 0) {
                    return "输出：对 " + who + " 造成约 " + ev.damage + " 点伤害";
                }
                return "输出：先手压制 " + who;
            }
            case 1: {
                if (pending > 0) {
                    return "格挡：补 " + ev.rawBlock + " 点，挡住本回合 " + pending
                            + " 点伤害中的 " + Math.min(ev.rawBlock, Math.max(0, pending - ctx.block)) + " 点";
                }
                return "格挡：先叠 " + ev.rawBlock + " 点护盾";
            }
            case 2: {
                return statusReason(who, ev);
            }
            case 3: {
                if (ev.mainPower == Status.STRENGTH) {
                    return "成长：力量 +" + Math.abs(ev.mainPowerAmount) + "，越早打出越赚";
                }
                String label = ev.mainPower == null ? "能力" : ev.mainPower.label;
                String amt = ev.mainPowerAmount == 0 ? "" : " " + Math.abs(ev.mainPowerAmount);
                return "成长：获得「" + label + amt + "」，越早打出收益越高";
            }
            case 4: {
                if (ev.draw > 0) {
                    return "过牌：抽 " + ev.draw + " 张" + (ctx.handSize - 1 <= 2 ? "，手牌少时最划算" : "");
                }
                if (ev.energyGain > 0) {
                    return "回能：+" + ev.energyGain + " 能量，够再打一手";
                }
                if (ev.heal > 0) {
                    return "回复：" + ev.heal + " 点生命";
                }
                return "周转：为下一张牌铺路";
            }
            default: {
                return "顺手打出「" + ev.card.name() + "」，不亏节奏";
            }
        }
    }

    private static String statusReason(String who, Eval ev) {
        Status s = ev.mainStatus;
        int amount = ev.mainStatusAmount;
        if (s == null) {
            return "干扰：削弱 " + who;
        }
        switch (s) {
            case VULNERABLE:
                return "易伤：对 " + who + " 施加 " + amount + " 层，为后续攻击铺垫";
            case WEAK:
                return "虚弱：对 " + who + " 施加 " + amount + " 层，少挨约 "
                        + Math.round(ev.status) + " 点伤害";
            case STRENGTH:
                if (amount < 0) {
                    return "缴械：让 " + who + " 少 " + (-amount) + " 点力量";
                }
                return "强化：给 " + who + " 加 " + amount + " 点力量";
            default:
                return "干扰：对 " + who + " 施加「" + s.label + " " + amount + "」";
        }
    }

    // ------------------------------------------------------------------
    // 小工具
    // ------------------------------------------------------------------

    private static ArrayList<Foe> snapshot(List<EnemyState> alive) {
        ArrayList<Foe> out = new ArrayList<>(alive.size());
        for (int i = 0; i < alive.size(); i++) {
            out.add(new Foe(alive.get(i)));
        }
        return out;
    }

    private static Foe foeOf(List<Foe> foes, EnemyState t) {
        if (t == null) {
            return null;
        }
        for (int i = 0; i < foes.size(); i++) {
            if (foes.get(i).e == t) {
                return foes.get(i);
            }
        }
        return null;
    }

    private static boolean allDead(List<Foe> foes) {
        for (int i = 0; i < foes.size(); i++) {
            if (foes.get(i).hp > 0) {
                return false;
            }
        }
        return true;
    }

    private static int pendingIncoming(CombatState c, List<Foe> foes) {
        int sum = 0;
        for (int i = 0; i < foes.size(); i++) {
            Foe f = foes.get(i);
            if (f.hp > 0) {
                sum += EnemyAi.estimateDamage(c, f.e, f.e.intent);
            }
        }
        return sum;
    }

    private static int hitsInIntent(EnemyState e) {
        if (e.intent == null) {
            return 0;
        }
        int hits = 0;
        for (Effect effect : e.intent.effects) {
            if (effect.kind == Effect.Kind.DAMAGE
                    || effect.kind == Effect.Kind.DAMAGE_ALL
                    || effect.kind == Effect.Kind.DAMAGE_RANDOM) {
                hits += effect.hits;
            }
        }
        return hits;
    }

    private static boolean needsEnemyTarget(CardInstance card) {
        if (card.def.target == Target.ENEMY) {
            return true;
        }
        List<Effect> effects = card.effects();
        for (int i = 0; i < effects.size(); i++) {
            Effect.Kind k = effects.get(i).kind;
            if (k == Effect.Kind.APPLY_ENEMY || k == Effect.Kind.DAMAGE || k == Effect.Kind.DAMAGE_BLOCK) {
                return true;
            }
        }
        return false;
    }

    private static boolean dealsDamage(CardInstance card) {
        List<Effect> effects = card.effects();
        for (int i = 0; i < effects.size(); i++) {
            Effect.Kind k = effects.get(i).kind;
            if (k == Effect.Kind.DAMAGE || k == Effect.Kind.DAMAGE_ALL
                    || k == Effect.Kind.DAMAGE_RANDOM || k == Effect.Kind.DAMAGE_X
                    || k == Effect.Kind.DAMAGE_BLOCK) {
                return true;
            }
        }
        return false;
    }

    private static boolean appliesStatus(CardInstance card, Status status) {
        List<Effect> effects = card.effects();
        for (int i = 0; i < effects.size(); i++) {
            Effect e = effects.get(i);
            if (e.status == status && e.amount > 0
                    && (e.kind == Effect.Kind.APPLY_ENEMY || e.kind == Effect.Kind.APPLY_ALL)) {
                return true;
            }
        }
        return false;
    }

    private static boolean appliesNegativeStrength(CardInstance card) {
        List<Effect> effects = card.effects();
        for (int i = 0; i < effects.size(); i++) {
            Effect e = effects.get(i);
            if (e.status == Status.STRENGTH && e.amount < 0
                    && (e.kind == Effect.Kind.APPLY_ENEMY || e.kind == Effect.Kind.APPLY_ALL)) {
                return true;
            }
        }
        return false;
    }

    private static int otherPlayableCost(Ctx ctx, CombatEngine engine, CardInstance card) {
        int sum = 0;
        for (int i = 0; i < ctx.c.hand.size(); i++) {
            CardInstance h = ctx.c.hand.get(i);
            if (h == card || h.def == null) {
                continue;
            }
            if (!engine.canPlay(h)) {
                continue;
            }
            int cost = engine.currentCost(h);
            if (cost > 0) {
                sum += cost;
            }
        }
        return sum;
    }

    private static int otherDrawCards(Ctx ctx, CardInstance card) {
        int n = 0;
        for (int i = 0; i < ctx.c.hand.size(); i++) {
            CardInstance h = ctx.c.hand.get(i);
            if (h == card || h.def == null) {
                continue;
            }
            List<Effect> effects = h.effects();
            for (int k = 0; k < effects.size(); k++) {
                if (effects.get(k).kind == Effect.Kind.DRAW) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    private static int countAttacks(CombatState c) {
        int n = 0;
        for (int i = 0; i < c.hand.size(); i++) {
            CardInstance h = c.hand.get(i);
            if (h.def != null && h.def.type == CardType.ATTACK) {
                n++;
            }
        }
        return n;
    }

    private static int countSkills(CombatState c) {
        int n = 0;
        for (int i = 0; i < c.hand.size(); i++) {
            CardInstance h = c.hand.get(i);
            if (h.def != null && h.def.type == CardType.SKILL) {
                n++;
            }
        }
        return n;
    }

    /** 敌人这回合一共会打你几段（有意图才计入）。 */
    private static int countEnemyHits(CombatState c) {
        int hits = 0;
        List<EnemyState> alive = c.living();
        for (int i = 0; i < alive.size(); i++) {
            hits += hitsInIntent(alive.get(i));
        }
        return hits;
    }

    private static int sumHp(List<EnemyState> alive) {
        int sum = 0;
        for (int i = 0; i < alive.size(); i++) {
            sum += Math.max(0, alive.get(i).hp + alive.get(i).block);
        }
        return sum;
    }

    private static int estimateHit(int base, int strMultiplier, int strength, boolean weak) {
        int dmg = base + strength * Math.max(1, strMultiplier);
        if (weak) {
            dmg = (int) Math.floor(dmg * 0.75);
        }
        return Math.max(0, dmg);
    }

    /** 手牌里的攻击牌大约还能打出多少伤害，用来给易伤 / 力量估值。 */
    private static int estimateAttackPotential(CombatState c) {
        int total = 0;
        int strength = c.playerStatus(Status.STRENGTH) + c.rAttackBonus;
        boolean weak = c.playerStatus(Status.WEAK) > 0;
        int living = Math.max(1, c.living().size());
        int energy = Math.max(0, c.energy);
        for (int i = 0; i < c.hand.size(); i++) {
            CardInstance card = c.hand.get(i);
            if (card.def == null || card.def.type != CardType.ATTACK) {
                continue;
            }
            List<Effect> effects = card.effects();
            for (int k = 0; k < effects.size(); k++) {
                Effect e = effects.get(k);
                switch (e.kind) {
                    case DAMAGE:
                    case DAMAGE_RANDOM:
                        total += estimateHit(e.amount, e.strengthMultiplier, strength, weak) * e.hits;
                        break;
                    case DAMAGE_ALL:
                        total += estimateHit(e.amount, e.strengthMultiplier, strength, weak) * e.hits * living;
                        break;
                    case DAMAGE_X:
                        total += estimateHit(e.amount, e.strengthMultiplier, strength, weak) * energy * living;
                        break;
                    case DAMAGE_BLOCK:
                        total += Math.max(0, c.block);
                        break;
                    default:
                        break;
                }
            }
        }
        return total;
    }

    private static int avgAttackDamage(Ctx ctx) {
        if (ctx.attacksInHand <= 0) {
            return 0;
        }
        return Math.max(1, ctx.attackPotential / ctx.attacksInHand);
    }

    private static int bestAttackDamage(Ctx ctx) {
        int best = 0;
        int strength = ctx.strength + ctx.attackBonus;
        for (int i = 0; i < ctx.c.hand.size(); i++) {
            CardInstance h = ctx.c.hand.get(i);
            if (h.def == null || h.def.type != CardType.ATTACK) {
                continue;
            }
            List<Effect> effects = h.effects();
            for (int k = 0; k < effects.size(); k++) {
                Effect e = effects.get(k);
                if (e.kind == Effect.Kind.DAMAGE || e.kind == Effect.Kind.DAMAGE_RANDOM) {
                    int d = estimateHit(e.amount, e.strengthMultiplier, strength, ctx.weak) * e.hits;
                    if (d > best) {
                        best = d;
                    }
                }
            }
        }
        return best;
    }
}
