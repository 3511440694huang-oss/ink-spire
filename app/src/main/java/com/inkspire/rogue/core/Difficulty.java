package com.inkspire.rogue.core;

/**
 * 难度曲线：把「怪物图鉴里写的原始数值」按幕换算成实际数值。
 *
 * <p>这是唯一一处整体难度旋钮。保持它只有一个地方可调，是为了让内容作者可以按
 * 「同幕的相对强弱」写数字，而不用在四十个敌人里反复乘系数。
 *
 * <ul>
 *   <li>血量：越往后越接近 100%，因为玩家的输出随牌组成长本来就涨得更快。</li>
 *   <li>伤害：逐幕收紧，避免第二幕之后变成「一次意图打掉半管血」。</li>
 * </ul>
 */
public final class Difficulty {
    /** 每幕敌人血量的百分比。 */
    private static final int[] HP_PCT = {80, 72, 65, 58};
    /** 每幕敌人伤害的百分比。 */
    private static final int[] DMG_PCT = {74, 66, 59, 53};

    private Difficulty() {}

    private static int clamp(int act) {
        if (act < 0) return 0;
        if (act > HP_PCT.length - 1) return HP_PCT.length - 1;
        return act;
    }

    public static int hpPercent(int act) { return HP_PCT[clamp(act)]; }

    public static int damagePercent(int act) { return DMG_PCT[clamp(act)]; }

    public static int hp(int base, int act) {
        if (base <= 0) return base;
        int pct = hpPercent(act);
        if (pct == 100) return base;
        return Math.max(4, (int) Math.round(base * pct / 100.0));
    }
}
