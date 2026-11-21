package com.inkspire.rogue.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One atomic card or enemy move effect. Immutable. */
public final class Effect {
    public enum Kind {
        DAMAGE, DAMAGE_ALL, DAMAGE_RANDOM, DAMAGE_X, DAMAGE_BLOCK,
        BLOCK, DRAW, ENERGY, HEAL, LOSE_HP, MAX_HP,
        APPLY_ENEMY, APPLY_ALL, POWER, DOUBLE_STRENGTH, DOUBLE_BLOCK,
        EXHAUST_RANDOM, EXHAUST_NON_ATTACK_BLOCK
    }

    public final Kind kind;
    public final int amount;
    public final int hits;
    public final Status status;
    public final Target target; // ENEMY for single, ALL_ENEMIES for all, SELF for player
    public final int strengthMultiplier;

    private Effect(Kind kind, int amount, int hits, Status status, Target target, int strengthMultiplier) {
        this.kind = kind; this.amount = amount; this.hits = Math.max(1, hits);
        this.status = status; this.target = target;
        this.strengthMultiplier = Math.max(1, strengthMultiplier);
    }

    public static Effect damage(int amount) { return new Effect(Kind.DAMAGE, amount, 1, null, Target.ENEMY, 1); }
    public static Effect damage(int amount, int hits) { return new Effect(Kind.DAMAGE, amount, hits, null, Target.ENEMY, 1); }
    public static Effect heavy(int amount, int strengthMultiplier) { return new Effect(Kind.DAMAGE, amount, 1, null, Target.ENEMY, strengthMultiplier); }
    public static Effect damageAll(int amount) { return new Effect(Kind.DAMAGE_ALL, amount, 1, null, Target.ALL_ENEMIES, 1); }
    public static Effect damageRandom(int amount, int hits) { return new Effect(Kind.DAMAGE_RANDOM, amount, hits, null, Target.ENEMY, 1); }
    public static Effect damageX(int amount) { return new Effect(Kind.DAMAGE_X, amount, 1, null, Target.ALL_ENEMIES, 1); }
    public static Effect bodySlam() { return new Effect(Kind.DAMAGE_BLOCK, 0, 1, null, Target.ENEMY, 1); }
    public static Effect block(int amount) { return new Effect(Kind.BLOCK, amount, 1, null, Target.SELF, 1); }
    public static Effect draw(int amount) { return new Effect(Kind.DRAW, amount, 1, null, Target.SELF, 1); }
    public static Effect energy(int amount) { return new Effect(Kind.ENERGY, amount, 1, null, Target.SELF, 1); }
    public static Effect heal(int amount) { return new Effect(Kind.HEAL, amount, 1, null, Target.SELF, 1); }
    public static Effect loseHp(int amount) { return new Effect(Kind.LOSE_HP, amount, 1, null, Target.SELF, 1); }
    public static Effect maxHp(int amount) { return new Effect(Kind.MAX_HP, amount, 1, null, Target.SELF, 1); }
    public static Effect apply(Status status, int amount) { return new Effect(Kind.APPLY_ENEMY, amount, 1, status, Target.ENEMY, 1); }
    public static Effect applyAll(Status status, int amount) { return new Effect(Kind.APPLY_ALL, amount, 1, status, Target.ALL_ENEMIES, 1); }
    public static Effect power(Status status, int amount) { return new Effect(Kind.POWER, amount, 1, status, Target.SELF, 1); }
    public static Effect doubleStrength() { return new Effect(Kind.DOUBLE_STRENGTH, 1, 1, null, Target.SELF, 1); }
    public static Effect doubleBlock() { return new Effect(Kind.DOUBLE_BLOCK, 1, 1, null, Target.SELF, 1); }
    public static Effect exhaustRandom(int amount) { return new Effect(Kind.EXHAUST_RANDOM, amount, 1, null, Target.SELF, 1); }
    public static Effect exhaustNonAttackBlock(int amount) { return new Effect(Kind.EXHAUST_NON_ATTACK_BLOCK, amount, 1, null, Target.SELF, 1); }

    public static List<Effect> of(Effect... effects) { return new ArrayList<>(Arrays.asList(effects)); }

    public String describe() {
        switch (kind) {
            case DAMAGE: return (hits > 1 ? hits + "×" + amount : amount + "") + "伤害";
            case DAMAGE_ALL: return amount + "伤害(全体)";
            case DAMAGE_RANDOM: return hits + "×" + amount + "随机伤害";
            case DAMAGE_X: return "X×" + amount + "伤害(全体)";
            case DAMAGE_BLOCK: return "伤害=格挡";
            case BLOCK: return amount + "格挡";
            case DRAW: return "抽" + amount + "张";
            case ENERGY: return "+" + amount + "能量";
            case HEAL: return "回复" + amount;
            case LOSE_HP: return "失去" + amount + "生命";
            case MAX_HP: return "+" + amount + "最大生命";
            case APPLY_ENEMY: return status.label + (amount >= 0 ? "+" : "") + amount;
            case APPLY_ALL: return "全体" + status.label + (amount >= 0 ? "+" : "") + amount;
            case POWER: return status.label + "+" + amount;
            case DOUBLE_STRENGTH: return "力量翻倍";
            case DOUBLE_BLOCK: return "格挡翻倍";
            case EXHAUST_RANDOM: return "消耗" + amount + "张随机手牌";
            case EXHAUST_NON_ATTACK_BLOCK: return "每消耗1张非攻击牌+" + amount + "格挡";
            default: return "";
        }
    }
}
