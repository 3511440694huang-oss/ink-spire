package com.inkspire.rogue.core;

public enum NodeType {
    BATTLE("战", "战斗"), ELITE("精", "精英"), EVENT("事", "事件"),
    REST("火", "营火"), SHOP("店", "商店"), TREASURE("宝", "宝箱"), BOSS("王", "首领");
    public final String glyph;
    public final String label;
    NodeType(String glyph, String label) { this.glyph = glyph; this.label = label; }
}
