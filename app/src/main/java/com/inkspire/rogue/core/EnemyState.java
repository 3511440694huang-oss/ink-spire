package com.inkspire.rogue.core;

import com.inkspire.rogue.model.EnemyDef;
import com.inkspire.rogue.model.MoveDef;
import com.inkspire.rogue.model.Status;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Random;

public final class EnemyState {
    public final EnemyDef def;
    public int hp, maxHp, block;
    public final EnumMap<Status, Integer> statuses = new EnumMap<>(Status.class);
    public MoveDef intent;
    /** 伤害系数（百分比）。由 EnemyLibrary 按幕设置，用于整体难度微调。 */
    public int damagePercent = 100;
    public int turnsAlive;
    public int lastMoveTurn = -99;
    public String lastMoveId = "";
    public int repeatCount;
    public final ArrayList<String> moveHistory = new ArrayList<>();

    public EnemyState(EnemyDef def, int hp, Random rng) {
        this.def = def;
        this.damagePercent = Difficulty.damagePercent(def == null ? 0 : def.act);
        int low = def == null ? Math.max(1, hp) : def.hpMin;
        int high = def == null ? Math.max(1, hp) : def.hpMax;
        int rolled = low;
        if (high > low && rng != null) rolled = low + rng.nextInt(high - low + 1);
        else if (hp > 0) rolled = hp;
        this.maxHp = Math.max(1, Difficulty.hp(rolled, def == null ? 0 : def.act));
        this.hp = this.maxHp;
    }

    public boolean alive() { return hp > 0; }

    /** 供 AI 推演用的独立副本：数值、状态、意图、招式历史都复制一份。 */
    public EnemyState copy() {
        EnemyState e = new EnemyState(def, Math.max(1, hp), null);
        e.hp = hp;
        e.maxHp = maxHp;
        e.block = block;
        e.statuses.putAll(statuses);
        e.intent = intent;
        e.damagePercent = damagePercent;
        e.turnsAlive = turnsAlive;
        e.lastMoveTurn = lastMoveTurn;
        e.lastMoveId = lastMoveId;
        e.repeatCount = repeatCount;
        e.moveHistory.addAll(moveHistory);
        return e;
    }

    /** 把敌人招式里写的伤害值按当前难度系数换算。意图显示与实际结算共用这一处。 */
    public int scaledDamage(int base) {
        if (damagePercent == 100) return base;
        int v = (int) Math.round(base * damagePercent / 100.0);
        return Math.max(1, v);
    }
    public int status(Status s) { Integer v = statuses.get(s); return v == null ? 0 : v; }
    public void addStatus(Status s, int amount) {
        if (amount == 0) return;
        int now = status(s) + amount;
        if (now == 0) statuses.remove(s); else statuses.put(s, now);
    }
    public void clearBlock() { block = 0; }
    public void damage(int amount) {
        if (amount <= 0) return;
        hp = Math.max(0, hp - amount);
    }
    public void heal(int amount) {
        if (amount <= 0) return;
        hp = Math.min(maxHp, hp + amount);
    }
    public void recordMove(MoveDef m, int turn) {
        if (m == null) return;
        turnsAlive++;
        if (m.id.equals(lastMoveId)) repeatCount++; else repeatCount = 1;
        lastMoveId = m.id;
        lastMoveTurn = turn;
        moveHistory.add(m.id);
        if (moveHistory.size() > 12) moveHistory.remove(0);
    }
    public int repeatOf(String id) {
        int n = 0;
        for (int i = moveHistory.size() - 1; i >= 0; i--) {
            if (moveHistory.get(i).equals(id)) n++; else break;
        }
        return n;
    }
    public int timesUsed(String id) {
        int n = 0;
        for (String s : moveHistory) if (s.equals(id)) n++;
        return n;
    }
}
