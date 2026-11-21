package com.inkspire.rogue.model;

public enum CardType {
    ATTACK("攻击"), SKILL("技能"), POWER("能力"), CURSE("诅咒"), STATUS("状态");
    public final String label;
    CardType(String label) { this.label = label; }
}
