package com.inkspire.rogue.core;

import com.inkspire.rogue.BuildConfig;

/** Runtime cheats only compiled into the cheat flavor's menu; harmless in the standard build. */
public final class Cheats {
    public static final boolean AVAILABLE = BuildConfig.CHEAT;
    public static boolean infiniteEnergy;
    public static boolean invincible;
    public static boolean oneHit;
    public static boolean infiniteGold;
    public static boolean freeCards;
    public static boolean autoWin;
    private Cheats() {}
    public static void reset() {
        infiniteEnergy = false; invincible = false; oneHit = false;
        infiniteGold = false; freeCards = false; autoWin = false;
    }
}
