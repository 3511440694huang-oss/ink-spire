package com.inkspire.rogue.model;

public enum Rarity {
    BASIC("初始"), COMMON("普通"), UNCOMMON("罕见"), RARE("稀有"), SPECIAL("特殊");
    public final String label;
    Rarity(String label) { this.label = label; }
}
