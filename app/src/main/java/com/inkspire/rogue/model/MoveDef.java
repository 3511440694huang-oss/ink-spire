package com.inkspire.rogue.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MoveDef {
    public final String id, name;
    /** Optional hand-written intent; if empty the engine derives it from effects. */
    public final String intentLabel;
    public final List<Effect> effects;
    public final int weight;
    public final int cooldown;
    public final int minTurn;
    public final int maxRepeat;
    /** 0 = always; 2 = only below 50% HP (phase two). */
    public final int phase;

    public MoveDef(String id, String name, String intentLabel, List<Effect> effects,
                   int weight, int cooldown, int minTurn, int maxRepeat, int phase) {
        this.id = id; this.name = name; this.intentLabel = intentLabel;
        this.effects = Collections.unmodifiableList(new ArrayList<>(effects));
        this.weight = weight; this.cooldown = cooldown; this.minTurn = minTurn;
        this.maxRepeat = maxRepeat; this.phase = phase;
    }

    public static MoveDef attack(String id, String name, int damage) {
        return new MoveDef(id, name, "", Effect.of(Effect.damage(damage)), 10, 0, 0, 2, 0);
    }
    public static MoveDef attack(String id, String name, int damage, int weight) {
        return new MoveDef(id, name, "", Effect.of(Effect.damage(damage)), weight, 0, 0, 2, 0);
    }
    public static MoveDef of(String id, String name, int weight, Effect... effects) {
        List<Effect> list = Effect.of(effects);
        return new MoveDef(id, name, "", list, weight, 0, 0, 2, 0);
    }
    public MoveDef phase2() { return new MoveDef(id, name, intentLabel, effects, weight, cooldown, minTurn, maxRepeat, 2); }
    public MoveDef cooldown(int turns) { return new MoveDef(id, name, intentLabel, effects, weight, turns, minTurn, maxRepeat, phase); }
    public MoveDef minTurn(int turn) { return new MoveDef(id, name, intentLabel, effects, weight, cooldown, turn, maxRepeat, phase); }
    public MoveDef maxRepeat(int times) { return new MoveDef(id, name, intentLabel, effects, weight, cooldown, minTurn, times, phase); }
    public MoveDef intent(String label) { return new MoveDef(id, name, label, effects, weight, cooldown, minTurn, maxRepeat, phase); }
}
