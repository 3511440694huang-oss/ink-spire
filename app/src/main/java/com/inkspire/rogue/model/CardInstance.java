package com.inkspire.rogue.model;

import java.util.concurrent.atomic.AtomicLong;

public final class CardInstance {
    private static final AtomicLong SEQ = new AtomicLong(1);
    public final long uid;
    public final CardDef def;
    public boolean upgraded;

    public CardInstance(CardDef def) { this(def, false); }
    public CardInstance(CardDef def, boolean upgraded) {
        this.uid = SEQ.getAndIncrement(); this.def = def; this.upgraded = upgraded;
    }
    public String name() { return upgraded ? def.plusName : def.name; }
    public int cost() { return upgraded ? def.plusCost : def.cost; }
    public java.util.List<Effect> effects() { return upgraded ? def.plusEffects : def.effects; }
    public String description() { return upgraded ? def.plusDescription : def.description; }
    public boolean exhaust() { return upgraded ? def.plusExhaust : def.exhaust; }
    public boolean noDraw() { return upgraded ? def.plusNoDraw : def.noDraw; }
    public CardInstance copy() { return new CardInstance(def, upgraded); }
}
