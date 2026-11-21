package com.inkspire.rogue.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EnemyDef {
    public enum Tier { NORMAL, ELITE, BOSS }
    public final String id, name, glyph, lore;
    public final Tier tier;
    public final int hpMin, hpMax;
    public final List<MoveDef> moves;
    public final int act;

    public EnemyDef(String id, String name, String glyph, String lore, Tier tier, int act, int hpMin, int hpMax, List<MoveDef> moves) {
        this.id = id; this.name = name; this.glyph = glyph; this.lore = lore;
        this.tier = tier; this.act = act; this.hpMin = hpMin; this.hpMax = hpMax;
        this.moves = Collections.unmodifiableList(new ArrayList<>(moves));
    }
}
