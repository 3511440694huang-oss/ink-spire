package com.inkspire.rogue.model;

/** Persistent combat statuses. */
public enum Status {
    STRENGTH("力量", false),
    DEXTERITY("敏捷", false),
    VULNERABLE("易伤", true),
    WEAK("虚弱", true),
    FRAIL("脆弱", true),
    THORNS("荆棘", false),
    METALLICIZE("金属化", false),
    REGEN("再生", false),
    DEMON_FORM("恶魔形态", false),
    CORRUPTION("腐化", false),
    BARRICADE("壁垒", false),
    FEEL_NO_PAIN("无惧", false),
    DARK_EMBRACE("黑暗之拥", false),
    EVOLVE("进化", false),
    RUPTURE("撕裂", false),
    FLAME_BARRIER("火焰屏障", true),
    DOUBLE_TAP("双发", true),
    RITUAL("仪式", false),
    JUGGERNAUT("势不可挡", false),
    BRUTALITY("残暴", false),
    BERSERK("狂暴", false),
    NO_DRAW("凝神", true),
    POISON("中毒", false),
    SHACKLE("枷锁", true);

    public final String label;
    /** Temporary debuffs/buffs that lose one stack at end of the owner's turn. */
    public final boolean turnsDown;
    Status(String label, boolean turnsDown) { this.label = label; this.turnsDown = turnsDown; }
}
