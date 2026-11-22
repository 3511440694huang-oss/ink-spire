package com.inkspire.rogue.model;

public final class PotionDef {
    public enum Kind {
        HEAL, STRENGTH, DEXTERITY, BLOCK, ENERGY, DRAW, FIRE,
        WEAK_ALL, POISON, REGEN, THORNS, MAX_HP, CLEANSE,
        IRON_SKIN, BLOCK_DRAW, HEAL_THORNS, WEAK_VULN_ALL
    }
    public final String id, name, glyph, description;
    public final Kind kind;
    public final int amount;
    public PotionDef(String id, String name, String glyph, String description, Kind kind, int amount) {
        this.id = id; this.name = name; this.glyph = glyph; this.description = description;
        this.kind = kind; this.amount = amount;
    }
}
