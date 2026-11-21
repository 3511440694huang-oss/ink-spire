package com.inkspire.rogue.ai;

import com.inkspire.rogue.core.CombatState;
import com.inkspire.rogue.core.EnemyState;
import com.inkspire.rogue.model.Effect;
import com.inkspire.rogue.model.MoveDef;
import com.inkspire.rogue.model.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility-scoring enemy brain. It reads the actual board (HP, block, strength, debuffs,
 * move history) instead of following a fixed script, then commits the chosen intent.
 * The intent is public information, so the player can plan around it.
 */
public final class EnemyAi {
    private EnemyAi() {}

    public static MoveDef choose(CombatState c, EnemyState self) {
        List<MoveDef> legal = new ArrayList<>();
        for (MoveDef m : self.def.moves) {
            if (m.phase == 2 && self.hp * 2 > self.maxHp) continue;
            if (m.minTurn > 0 && c.turn < m.minTurn) continue;
            if (m.cooldown > 0 && c.turn - self.lastMoveTurn <= m.cooldown) continue;
            if (m.maxRepeat > 0 && self.repeatOf(m.id) >= m.maxRepeat) continue;
            legal.add(m);
        }
        if (legal.isEmpty()) legal.addAll(self.def.moves);
        if (legal.isEmpty()) return null;

        MoveDef best = legal.get(0);
        double bestScore = -1e9;
        for (MoveDef m : legal) {
            double score = score(c, self, m);
            score += c.rng.nextDouble() * 0.4; // tiny tie-breaker, no player-visible randomness
            if (score > bestScore) { bestScore = score; best = m; }
        }
        return best;
    }

    public static double score(CombatState c, EnemyState self, MoveDef m) {
        int damage = estimateDamage(c, self, m);
        int block = estimateBlock(c, self, m);
        int heal = 0, buff = 0, debuff = 0, draw = 0;
        boolean attack = false;
        for (Effect e : m.effects) {
            if (e.kind == Effect.Kind.DAMAGE || e.kind == Effect.Kind.DAMAGE_ALL || e.kind == Effect.Kind.DAMAGE_RANDOM) attack = true;
            if (e.kind == Effect.Kind.BLOCK) block += e.amount;
            if (e.kind == Effect.Kind.HEAL) heal += e.amount;
            if (e.kind == Effect.Kind.POWER) buff += e.amount;
            if (e.kind == Effect.Kind.APPLY_ENEMY || e.kind == Effect.Kind.APPLY_ALL) {
                if (e.status == Status.STRENGTH && e.amount < 0) buff += -e.amount * 2;
                else if (e.status == Status.VULNERABLE || e.status == Status.WEAK || e.status == Status.FRAIL) debuff += e.amount * 3;
            }
            if (e.kind == Effect.Kind.DRAW) draw += e.amount;
        }
        playerHpFactor(c);
        double score = damage * 10.0;
        if (damage >= c.hp) score += 5000; // lethal is always correct
        double hpPct = self.hp / (double) Math.max(1, self.maxHp);
        // A hurt enemy values survival; a healthy one invests in scaling.
        score += block * (hpPct < 0.45 ? 11 : 4.5);
        score += heal * (hpPct < 0.55 ? 12 : 2);
        score += buff * (c.turn <= 3 ? 9 : 5);
        score += debuff * 8;
        // Don't waste a big attack into a wall of block unless it still connects.
        if (damage > 0 && c.block >= damage * 0.8) score -= damage * 2.5;
        // Multi-enemy coordination: if the player is already dead to another intent, this is moot.
        if (c.hp <= 0) score -= 100;
        // Reward variety and punish obvious loops.
        if (m.id.equals(self.lastMoveId)) score -= 12;
        if (self.repeatOf(m.id) >= 1) score -= 7;
        // Boss phase two gets a visible tempo boost.
        if (m.phase == 2) score += 18;
        if (m.cooldown > 0) score += 4; // set-up moves are usually worth taking
        if (attack && c.playerStatus(Status.VULNERABLE) > 0) score += damage;
        return score;
    }

    private static double playerHpFactor(CombatState c) { return c.hp / (double) Math.max(1, c.maxHp); }

    public static boolean isAttack(MoveDef m) {
        if (m == null) return false;
        for (Effect e : m.effects) {
            if (e.kind == Effect.Kind.DAMAGE || e.kind == Effect.Kind.DAMAGE_ALL || e.kind == Effect.Kind.DAMAGE_RANDOM) return true;
        }
        return false;
    }

    /**
     * 一次攻击每段的实际伤害。顺序与 CombatEngine.enemyHitPlayer 完全一致：
     * 基础 + 敌人力量 → 敌人虚弱 ×0.75 → 玩家易伤 ×1.5 → 向下取整、不低于 0。
     */
    public static int hitDamage(CombatState c, EnemyState self, int base) {
        int one = self.scaledDamage(base) + self.status(Status.STRENGTH);
        if (self.status(Status.WEAK) > 0) one = (int) Math.floor(one * 0.75);
        if (c.playerStatus(Status.VULNERABLE) > 0) one = (int) Math.floor(one * 1.5);
        return Math.max(0, one);
    }

    private static boolean isDamageEffect(Effect e) {
        return e.kind == Effect.Kind.DAMAGE
                || e.kind == Effect.Kind.DAMAGE_ALL
                || e.kind == Effect.Kind.DAMAGE_RANDOM;
    }

    /** Incoming damage shown in the intent, including strength, weak and vulnerability. */
    public static int estimateDamage(CombatState c, EnemyState self, MoveDef m) {
        if (m == null) return 0;
        int total = 0;
        for (Effect e : m.effects) {
            if (isDamageEffect(e)) total += hitDamage(c, self, e.amount) * e.hits;
        }
        return total;
    }

    /** 这一招会打几段（用于荆棘估值）。 */
    public static int hitsOf(MoveDef m) {
        if (m == null) return 0;
        int hits = 0;
        for (Effect e : m.effects) if (isDamageEffect(e)) hits += e.hits;
        return hits;
    }

    public static int estimateBlock(CombatState c, EnemyState self, MoveDef m) {
        if (m == null) return 0;
        int total = 0;
        for (Effect e : m.effects) {
            if (e.kind == Effect.Kind.BLOCK) {
                int one = e.amount + self.status(Status.DEXTERITY);
                if (self.status(Status.FRAIL) > 0) one = (int) Math.floor(one * 0.75);
                total += Math.max(0, one);
            }
        }
        return total;
    }

    /** Compact Chinese intent. Multi-effect intents are separated by a middle dot. */
    public static String intentText(CombatState c, EnemyState self) {
        if (self.intent == null) return "…";
        StringBuilder sb = new StringBuilder();
        int hits = 0;
        int perHit = 0;
        for (Effect e : self.intent.effects) {
            if (isDamageEffect(e)) {
                if (perHit == 0) perHit = hitDamage(c, self, e.amount);
                hits += e.hits;
            }
        }
        if (hits > 0 && perHit > 0) {
            sb.append("攻 ").append(perHit);
            if (hits > 1) sb.append("×").append(hits);
        }
        for (Effect e : self.intent.effects) {
            if (e.kind == Effect.Kind.BLOCK) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append("盾 ").append(estimateBlock(c, self, self.intent));
            } else if (e.kind == Effect.Kind.POWER) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(statusBuffLabel(e.status)).append(e.amount >= 0 ? "+" : "").append(e.amount);
            } else if (e.kind == Effect.Kind.APPLY_ENEMY || e.kind == Effect.Kind.APPLY_ALL) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(e.status.label).append(e.amount);
            } else if (e.kind == Effect.Kind.HEAL) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append("回 ").append(e.amount);
            } else if (e.kind == Effect.Kind.LOSE_HP) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append("自损 ").append(e.amount);
            }
        }
        if (sb.length() == 0) sb.append("蓄势");
        if (self.intent.phase == 2) sb.append(" ！");
        return sb.toString();
    }

    private static String statusBuffLabel(Status s) {
        switch (s) {
            case STRENGTH: return "力";
            case DEXTERITY: return "敏";
            case METALLICIZE: return "甲";
            case THORNS: return "棘";
            case REGEN: return "愈";
            case RITUAL: return "仪";
            default: return s.label;
        }
    }
}
