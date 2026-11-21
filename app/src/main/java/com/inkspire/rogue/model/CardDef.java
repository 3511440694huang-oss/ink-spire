package com.inkspire.rogue.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable card definition with a built-in upgraded variant. */
public final class CardDef {
    public final String id;
    public final String name;
    public final int cost;
    public final CardType type;
    public final Rarity rarity;
    public final Target target;
    public final String description;
    public final String flavor;
    public final List<Effect> effects;
    public final boolean exhaust;
    public final boolean ethereal;
    public final boolean innate;
    public final boolean retain;
    public final boolean xCost;
    public final boolean noDraw;
    public final boolean healUnblocked;
    public final boolean addCopyToDiscard;
    public final int onKillMaxHp;
    /** 诅咒/状态牌：回合结束时若仍在手牌，失去 N 生命。 */
    public final int handEndTurnLoseHp;
    /** 诅咒/状态牌：回合开始时若仍在手牌，获得 N 层虚弱。 */
    public final int handTurnStartWeak;
    public final String plusName;
    public final String plusDescription;
    public final int plusCost;
    public final List<Effect> plusEffects;
    public final boolean plusExhaust;
    public final boolean plusNoDraw;

    private CardDef(Builder b) {
        this.id = b.id; this.name = b.name; this.cost = b.cost; this.type = b.type;
        this.rarity = b.rarity; this.target = b.target;
        this.description = b.description; this.flavor = b.flavor;
        this.effects = Collections.unmodifiableList(new ArrayList<>(b.effects));
        this.exhaust = b.exhaust; this.ethereal = b.ethereal; this.innate = b.innate;
        this.retain = b.retain; this.xCost = b.xCost; this.noDraw = b.noDraw;
        this.healUnblocked = b.healUnblocked; this.addCopyToDiscard = b.addCopyToDiscard;
        this.onKillMaxHp = b.onKillMaxHp;
        this.handEndTurnLoseHp = b.handEndTurnLoseHp;
        this.handTurnStartWeak = b.handTurnStartWeak;
        this.plusName = b.plusName == null ? b.name + "+" : b.plusName;
        this.plusDescription = b.plusDescription == null ? b.description : b.plusDescription;
        this.plusCost = b.plusCost;
        this.plusEffects = Collections.unmodifiableList(new ArrayList<>(b.plusEffects.isEmpty() ? b.effects : b.plusEffects));
        this.plusExhaust = b.plusExhaust;
        this.plusNoDraw = b.plusNoDraw;
    }

    public CardDef upgraded() {
        Builder b = new Builder(id + "+", plusName, plusCost, type, rarity, target);
        b.description(plusDescription).effects(plusEffects).flavor(flavor)
         .exhaust(plusExhaust).ethereal(ethereal).innate(innate).retain(retain)
         .xCost(xCost).noDraw(plusNoDraw).healUnblocked(healUnblocked)
         .addCopyToDiscard(addCopyToDiscard).onKillMaxHp(onKillMaxHp)
         .handEndTurnLoseHp(handEndTurnLoseHp).handTurnStartWeak(handTurnStartWeak);
        return new CardDef(b);
    }

    public static Builder builder(String id, String name, int cost, CardType type, Rarity rarity, Target target) {
        return new Builder(id, name, cost, type, rarity, target);
    }

    public static final class Builder {
        final String id, name;
        final int cost;
        final CardType type;
        final Rarity rarity;
        final Target target;
        String description = "", flavor = "";
        List<Effect> effects = new ArrayList<>();
        boolean exhaust, ethereal, innate, retain, xCost, noDraw, healUnblocked, addCopyToDiscard;
        int onKillMaxHp;
        int handEndTurnLoseHp, handTurnStartWeak;
        String plusName, plusDescription;
        int plusCost;
        List<Effect> plusEffects = new ArrayList<>();
        boolean plusExhaust, plusNoDraw;

        Builder(String id, String name, int cost, CardType type, Rarity rarity, Target target) {
            this.id = id; this.name = name; this.cost = cost; this.type = type;
            this.rarity = rarity; this.target = target; this.plusCost = cost;
        }
        public Builder description(String v) { description = v; return this; }
        public Builder flavor(String v) { flavor = v; return this; }
        public Builder effects(List<Effect> v) { effects = v; return this; }
        public Builder effects(Effect... v) { effects = Effect.of(v); return this; }
        public Builder exhaust() { exhaust = true; return this; }
        public Builder exhaust(boolean v) { exhaust = v; return this; }
        public Builder ethereal() { ethereal = true; return this; }
        public Builder ethereal(boolean v) { ethereal = v; return this; }
        public Builder innate() { innate = true; return this; }
        public Builder innate(boolean v) { innate = v; return this; }
        public Builder retain() { retain = true; return this; }
        public Builder retain(boolean v) { retain = v; return this; }
        public Builder xCost() { xCost = true; return this; }
        public Builder xCost(boolean v) { xCost = v; return this; }
        public Builder noDraw() { noDraw = true; return this; }
        public Builder noDraw(boolean v) { noDraw = v; return this; }
        public Builder healUnblocked() { healUnblocked = true; return this; }
        public Builder healUnblocked(boolean v) { healUnblocked = v; return this; }
        public Builder addCopyToDiscard() { addCopyToDiscard = true; return this; }
        public Builder addCopyToDiscard(boolean v) { addCopyToDiscard = v; return this; }
        public Builder onKillMaxHp(int v) { onKillMaxHp = v; return this; }
        public Builder handEndTurnLoseHp(int v) { handEndTurnLoseHp = v; return this; }
        public Builder handTurnStartWeak(int v) { handTurnStartWeak = v; return this; }
        public Builder plus(String name, String desc, int cost, List<Effect> effects) {
            plusName = name; plusDescription = desc; plusCost = cost; plusEffects = effects; return this;
        }
        public Builder plus(String name, String desc, int cost, Effect... effects) {
            return plus(name, desc, cost, Effect.of(effects));
        }
        public Builder plusExhaust() { plusExhaust = true; return this; }
        public Builder plusExhaust(boolean v) { plusExhaust = v; return this; }
        public Builder plusNoDraw() { plusNoDraw = true; return this; }
        public CardDef build() { return new CardDef(this); }
    }
}
