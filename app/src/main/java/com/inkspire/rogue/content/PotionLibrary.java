package com.inkspire.rogue.content;

import com.inkspire.rogue.model.PotionDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 墨塔里能被装瓶、被喝下的十六种墨。引擎按 kind 结算。 */
public final class PotionLibrary {
    private static final Map<String, PotionDef> POTIONS = new LinkedHashMap<>();

    static {
        /* ---- 恢复与防护 ---- */
        add(new PotionDef("healing_ink", "愈合墨", "愈",
                "回复 20 点生命。", PotionDef.Kind.HEAL, 20));
        add(new PotionDef("great_healing_ink", "大愈合墨", "医",
                "回复 40 点生命。", PotionDef.Kind.HEAL, 40));
        add(new PotionDef("block_ink", "石肤墨", "盾",
                "获得 12 点格挡。", PotionDef.Kind.BLOCK, 12));
        add(new PotionDef("regen_ink", "生机墨", "生",
                "获得 4 层再生：每回合开始回复 4 点生命。", PotionDef.Kind.REGEN, 4));
        add(new PotionDef("thorn_ink", "棘皮墨", "棘",
                "获得 4 层荆棘：受到攻击时反伤 4 点。", PotionDef.Kind.THORNS, 4));
        add(new PotionDef("vigor_ink", "血骨墨", "骨",
                "最大生命 +4，并回复 4 点生命。", PotionDef.Kind.MAX_HP, 4));
        add(new PotionDef("cleanse_ink", "洗墨", "洗",
                "清除自身所有减益（虚弱、易伤、脆弱、中毒）。", PotionDef.Kind.CLEANSE, 1));

        /* ---- 强化 ---- */
        add(new PotionDef("strength_ink", "蛮力墨", "力",
                "获得 2 点力量。", PotionDef.Kind.STRENGTH, 2));
        add(new PotionDef("fury_ink", "狂怒墨", "怒",
                "获得 4 点力量。", PotionDef.Kind.STRENGTH, 4));
        add(new PotionDef("dexterity_ink", "轻身墨", "轻",
                "获得 2 点敏捷。", PotionDef.Kind.DEXTERITY, 2));

        /* ---- 节奏 ---- */
        add(new PotionDef("energy_ink", "涌泉墨", "涌",
                "获得 2 点能量。", PotionDef.Kind.ENERGY, 2));
        add(new PotionDef("draw_ink", "疾读墨", "阅",
                "抽 3 张牌。", PotionDef.Kind.DRAW, 3));

        /* ---- 攻击 ---- */
        add(new PotionDef("fire_ink", "爆燃墨", "燃",
                "对所有敌人造成 20 点基础伤害（受力量与易伤加成）。", PotionDef.Kind.FIRE, 20));
        add(new PotionDef("weak_ink", "滞涩墨", "滞",
                "对所有敌人施加 2 层虚弱。", PotionDef.Kind.WEAK_ALL, 2));

        /* ---- 复合 ---- */
        add(new PotionDef("iron_ink", "铁墨", "铁",
                "获得 1 点力量与 1 点敏捷。", PotionDef.Kind.IRON_SKIN, 1));
        add(new PotionDef("veil_ink", "蔽目墨", "蔽",
                "获得 10 点格挡，抽 2 张牌。", PotionDef.Kind.BLOCK_DRAW, 10));
        add(new PotionDef("ember_ink", "余温墨", "余",
                "回复 12 点生命，获得 3 层荆棘。", PotionDef.Kind.HEAL_THORNS, 12));
        add(new PotionDef("fog_ink", "迷雾墨", "雾",
                "使所有敌人获得 2 层虚弱与 2 层易伤。", PotionDef.Kind.WEAK_VULN_ALL, 2));

        /* ---- 毒 ---- */
        add(new PotionDef("poison_ink", "蚀心墨", "蚀",
                "使所有敌人获得 5 层中毒。", PotionDef.Kind.POISON, 5));
        add(new PotionDef("venom_ink", "剧毒墨", "毒",
                "使所有敌人获得 8 层中毒。", PotionDef.Kind.POISON, 8));
    }

    private PotionLibrary() {}

    private static void add(PotionDef potion) {
        POTIONS.put(potion.id, potion);
    }

    public static List<PotionDef> all() { return new ArrayList<>(POTIONS.values()); }

    public static PotionDef get(String id) {
        if (id == null) return null;
        return POTIONS.get(id);
    }

    public static PotionDef random(Random rng) {
        if (POTIONS.isEmpty()) return null;
        Random r = rng == null ? new Random() : rng;
        List<PotionDef> pool = new ArrayList<>(POTIONS.values());
        return pool.get(r.nextInt(pool.size()));
    }

    public static List<PotionDef> shopChoices(Random rng, int count) {
        List<PotionDef> pool = new ArrayList<>(POTIONS.values());
        Collections.shuffle(pool, rng);
        int n = Math.min(Math.max(0, count), pool.size());
        return Collections.unmodifiableList(new ArrayList<>(pool.subList(0, n)));
    }
}
