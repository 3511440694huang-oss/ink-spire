package com.inkspire.rogue.content;

import com.inkspire.rogue.model.Rarity;
import com.inkspire.rogue.model.RelicDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 墨塔的遗物。绝大多数遗物只是"填数字"，由引擎在固定时机统一结算；
 * 只有笔尖、死灵之书、三枚蛋因为要改出牌规则，由 CombatEngine / GameView 按 id 特判。
 */
public final class RelicLibrary {
    private static final Map<String, RelicDef> RELICS = new LinkedHashMap<>();

    static {
        /* ============ 角色初始遗物 ============ */
        add(RelicDef.of("burning_blood", "燃烧之血", "血",
                "战斗结束后回复 6 点生命。", Rarity.BASIC, RelicDef.Hook.REWARD)
                .postCombatHeal(6).build());
        add(RelicDef.of("jade_pendant", "青玉坠", "玉",
                "战斗开始时获得 1 点敏捷。", Rarity.BASIC, RelicDef.Hook.COMBAT_START)
                .startDexterity(1).build());
        add(RelicDef.of("ember_box", "余烬匣", "匣",
                "战斗开始时失去 2 点生命，获得 2 点力量。", Rarity.BASIC, RelicDef.Hook.COMBAT_START)
                .startStrength(2).loseHpStart(2).build());

        /* ============ 普通 ============ */
        add(RelicDef.of("anchor", "铁锚", "锚",
                "战斗开始时获得 10 点格挡。", Rarity.COMMON, RelicDef.Hook.COMBAT_START)
                .startBlock(10).build());
        add(RelicDef.of("lantern", "提灯", "灯",
                "每场战斗第一回合额外获得 1 点能量。", Rarity.COMMON, RelicDef.Hook.COMBAT_START)
                .firstTurnEnergy(1).build());
        add(RelicDef.of("bag_of_prep", "预备行囊", "囊",
                "每场战斗第一回合多抽 2 张牌。", Rarity.COMMON, RelicDef.Hook.COMBAT_START)
                .startDraw(2).build());
        add(RelicDef.of("vajra", "金刚杵", "杵",
                "战斗开始时获得 1 点力量。", Rarity.COMMON, RelicDef.Hook.COMBAT_START)
                .startStrength(1).build());
        add(RelicDef.of("blood_vial", "血瓶", "瓶",
                "拾取时最大生命 +2。", Rarity.COMMON, RelicDef.Hook.PICKUP)
                .maxHp(2).build());
        add(RelicDef.of("oddly_smooth_stone", "光滑怪石", "石",
                "战斗开始时获得 1 点敏捷与 4 点格挡。", Rarity.COMMON, RelicDef.Hook.COMBAT_START)
                .startDexterity(1).startBlock(4).build());
        add(RelicDef.of("iron_helm", "凹痕铁盔", "盔",
                "战斗开始时获得 6 点格挡。战斗结束后回复 2 点生命。",
                Rarity.COMMON, RelicDef.Hook.COMBAT_START)
                .startBlock(6).postCombatHeal(2).build());
        add(RelicDef.of("whetstone", "磨石", "磨",
                "你的每一次攻击额外造成 1 点伤害。", Rarity.COMMON, RelicDef.Hook.CARD_PLAY)
                .attackBonus(1).build());
        add(RelicDef.of("pilgrim_boots", "行者靴", "靴",
                "在营火休息时额外回复 10 点生命。", Rarity.COMMON, RelicDef.Hook.REST)
                .restHeal(10).build());
        add(RelicDef.of("potion_belt", "药水腰带", "带",
                "药水栏 +1。", Rarity.COMMON, RelicDef.Hook.PICKUP)
                .potionSlots(1).build());
        add(RelicDef.of("thorn_charm", "带刺护符", "刺",
                "战斗开始时获得 3 点荆棘。", Rarity.COMMON, RelicDef.Hook.COMBAT_START)
                .startThorns(3).build());
        add(RelicDef.of("note_shard", "残笺", "笺",
                "每场战斗第一回合多抽 1 张牌。", Rarity.COMMON, RelicDef.Hook.COMBAT_START)
                .startDraw(1).build());
        add(RelicDef.of("wax_seal", "火漆印", "漆",
                "拾取时最大生命 +3。", Rarity.COMMON, RelicDef.Hook.PICKUP)
                .maxHp(3).build());
        add(RelicDef.of("dry_brush", "秃笔", "笔",
                "战斗结束后回复 3 点生命。", Rarity.COMMON, RelicDef.Hook.REWARD)
                .postCombatHeal(3).build());
        add(RelicDef.of("tin_lamp", "锡灯", "锡",
                "战斗开始时获得 1 点力量与 2 点格挡。", Rarity.COMMON, RelicDef.Hook.COMBAT_START)
                .startStrength(1).startBlock(2).build());

        /* ============ 罕见 ============ */
        add(RelicDef.of("thorns_relic", "荆棘冠", "棘",
                "战斗开始时获得 3 点荆棘。每回合结束时获得 2 点格挡。",
                Rarity.UNCOMMON, RelicDef.Hook.COMBAT_START)
                .startThorns(3).blockPerTurn(2).build());
        add(RelicDef.of("regal_pillow", "帝王枕", "枕",
                "在营火休息时额外回复 15 点生命。", Rarity.UNCOMMON, RelicDef.Hook.REST)
                .restHeal(15).build());
        add(RelicDef.of("golden_idol", "金铸神像", "像",
                "获得的金币增加 25%。", Rarity.UNCOMMON, RelicDef.Hook.GOLD)
                .goldPercent(25).build());
        add(RelicDef.of("pen_nib", "笔尖", "尖",
                "每打出 10 张攻击牌，下一张攻击牌伤害翻倍。", Rarity.UNCOMMON, RelicDef.Hook.CARD_PLAY)
                .penNib().build());
        add(RelicDef.of("meat_on_bone", "带肉骨头", "骨",
                "战斗结束后若生命值低于一半，额外回复 12 点生命。", Rarity.UNCOMMON, RelicDef.Hook.REWARD)
                .postCombatHealLow(12).build());
        add(RelicDef.of("toxic_egg", "毒蛋", "毒",
                "拾取后，之后获得的技能牌自动升级。", Rarity.UNCOMMON, RelicDef.Hook.PICKUP)
                .eggSkill().build());
        add(RelicDef.of("molten_egg", "熔蛋", "熔",
                "拾取后，之后获得的攻击牌自动升级。", Rarity.UNCOMMON, RelicDef.Hook.PICKUP)
                .eggAttack().build());
        add(RelicDef.of("frozen_egg", "冰蛋", "冰",
                "拾取后，之后获得的能力牌自动升级。", Rarity.UNCOMMON, RelicDef.Hook.PICKUP)
                .eggPower().build());
        add(RelicDef.of("darkstone_periapt", "黑石坠饰", "坠",
                "战斗开始时获得 2 点敏捷。", Rarity.UNCOMMON, RelicDef.Hook.COMBAT_START)
                .startDexterity(2).build());
        add(RelicDef.of("war_drum", "旧战鼓", "鼓",
                "战斗开始时获得 2 点力量。", Rarity.UNCOMMON, RelicDef.Hook.COMBAT_START)
                .startStrength(2).build());
        add(RelicDef.of("bronze_scales", "铜鳞", "鳞",
                "战斗开始时获得 4 点荆棘。", Rarity.UNCOMMON, RelicDef.Hook.COMBAT_START)
                .startThorns(4).build());
        add(RelicDef.of("merchant_ledger", "墨贩账本", "账",
                "商店里的所有商品打七五折。", Rarity.UNCOMMON, RelicDef.Hook.NONE)
                .shopDiscount(25).build());
        add(RelicDef.of("ink_pouch", "随身墨囊", "囊",
                "每当你打出一张牌，获得 1 点格挡。", Rarity.UNCOMMON, RelicDef.Hook.CARD_PLAY)
                .cardBlock(1).build());
        add(RelicDef.of("charcoal", "冷炭", "炭",
                "战斗开始时使所有敌人获得 3 层中毒。", Rarity.UNCOMMON, RelicDef.Hook.COMBAT_START)
                .startPoison(3).build());
        add(RelicDef.of("twin_lantern", "双灯", "灯",
                "每场战斗第一回合额外获得 2 点能量。", Rarity.UNCOMMON, RelicDef.Hook.COMBAT_START)
                .firstTurnEnergy(2).build());
        add(RelicDef.of("feather_quill", "灰羽笔", "羽",
                "每场战斗第一回合多抽 3 张牌。", Rarity.UNCOMMON, RelicDef.Hook.COMBAT_START)
                .startDraw(3).build());
        add(RelicDef.of("bloodstone", "血石", "石",
                "战斗开始时失去 3 点生命，获得 1 点力量与 1 点敏捷。",
                Rarity.UNCOMMON, RelicDef.Hook.COMBAT_START)
                .loseHpStart(3).startStrength(1).startDexterity(1).build());
        add(RelicDef.of("calm_bell", "定神铃", "铃",
                "每回合结束时获得 3 点格挡。", Rarity.UNCOMMON, RelicDef.Hook.TURN_START)
                .blockPerTurn(3).build());
        add(RelicDef.of("ink_stone", "墨玉砚", "砚",
                "每当你打出一张牌，获得 2 点格挡。", Rarity.UNCOMMON, RelicDef.Hook.CARD_PLAY)
                .cardBlock(2).build());
        add(RelicDef.of("moth_wing", "蛾翼", "蛾",
                "战斗开始时失去 2 点生命，获得 2 点敏捷。",
                Rarity.UNCOMMON, RelicDef.Hook.COMBAT_START)
                .loseHpStart(2).startDexterity(2).build());
        add(RelicDef.of("bone_whistle", "骨哨", "哨",
                "战斗开始时失去 2 点生命，获得 3 点力量。",
                Rarity.UNCOMMON, RelicDef.Hook.COMBAT_START)
                .loseHpStart(2).startStrength(3).build());
        add(RelicDef.of("worn_compass", "旧罗盘", "罗",
                "获得的金币增加 15%。", Rarity.UNCOMMON, RelicDef.Hook.GOLD)
                .goldPercent(15).build());
        add(RelicDef.of("page_marker", "护页签", "签",
                "每当你抽到一张诅咒或状态牌，获得 3 点格挡。",
                Rarity.UNCOMMON, RelicDef.Hook.NONE)
                .statusBlock(3).build());

        /* ============ 稀有 ============ */
        add(RelicDef.of("necronomicon", "死灵之书", "书",
                "每场战斗中第一张能力牌的费用变为 0。", Rarity.RARE, RelicDef.Hook.COMBAT_START)
                .freeFirstPower().build());
        add(RelicDef.of("prayer_wheel", "祈祷轮", "轮",
                "战斗胜利后的卡牌奖励多展示一张。", Rarity.RARE, RelicDef.Hook.REWARD)
                .eliteCardBonus(1).build());
        add(RelicDef.of("alchemist_pouch", "炼金药匣", "匣",
                "药水栏 +2。", Rarity.RARE, RelicDef.Hook.PICKUP)
                .potionSlots(2).build());
        add(RelicDef.of("silver_tongue", "银舌", "舌",
                "获得的金币增加 50%。", Rarity.RARE, RelicDef.Hook.GOLD)
                .goldPercent(50).build());
        add(RelicDef.of("venom_gland", "毒腺", "腺",
                "每当你打出攻击牌，使目标获得 1 层中毒。", Rarity.RARE, RelicDef.Hook.CARD_PLAY)
                .cardPoison(1).build());
        add(RelicDef.of("hourglass", "倒转沙漏", "沙",
                "每回合开始时获得 1 点力量。", Rarity.RARE, RelicDef.Hook.TURN_START)
                .strengthPerTurn(1).build());
        add(RelicDef.of("ritual_dagger", "仪刀", "刀",
                "每回合结束时获得 4 点格挡。", Rarity.RARE, RelicDef.Hook.TURN_START)
                .blockPerTurn(4).build());
        add(RelicDef.of("ink_well_relic", "干涸墨井", "井",
                "每回合额外获得 1 点能量。", Rarity.RARE, RelicDef.Hook.TURN_START)
                .energyPerTurn(1).build());
        add(RelicDef.of("grinding_stone", "磨盘", "盘",
                "你的每一次攻击额外造成 2 点伤害。", Rarity.RARE, RelicDef.Hook.CARD_PLAY)
                .attackBonus(2).build());
        add(RelicDef.of("sacred_bark", "圣树皮", "皮",
                "药水栏 +1，战斗结束后回复 3 点生命。", Rarity.RARE, RelicDef.Hook.REWARD)
                .potionSlots(1).postCombatHeal(3).build());
        add(RelicDef.of("dead_branch", "枯枝", "枝",
                "每当你打出一张牌，获得 3 点格挡。", Rarity.RARE, RelicDef.Hook.CARD_PLAY)
                .cardBlock(3).build());
        add(RelicDef.of("needle_spool", "缝针线轴", "针",
                "每回合开始时获得 1 点敏捷。", Rarity.RARE, RelicDef.Hook.TURN_START)
                .dexterityPerTurn(1).build());
        add(RelicDef.of("first_ink", "初墨", "初",
                "每场战斗你的第一张攻击牌额外造成 8 点伤害。",
                Rarity.RARE, RelicDef.Hook.CARD_PLAY)
                .firstAttackBonus(8).build());
        add(RelicDef.of("bitter_draught", "苦饮", "苦",
                "每当你使用一瓶药水，回复 5 点生命。", Rarity.RARE, RelicDef.Hook.NONE)
                .potionHeal(5).build());
        add(RelicDef.of("silent_tide", "静潮", "潮",
                "战斗开始时使所有敌人获得 2 层虚弱，并多抽 1 张牌。",
                Rarity.RARE, RelicDef.Hook.COMBAT_START)
                .startWeakAll(2).startDraw(1).build());
    }

    private RelicLibrary() {}

    private static void add(RelicDef relic) {
        RELICS.put(relic.id, relic);
    }

    public static List<RelicDef> all() {
        return new ArrayList<>(RELICS.values());
    }

    public static RelicDef get(String id) {
        if (id == null) return null;
        return RELICS.get(id);
    }

    /**
     * One random relic the player does not own yet. The starter relic is never rolled as a
     * reward, and rarer relics are less likely to appear. Returns null when nothing is left.
     */
    public static RelicDef random(Random rng, List<String> owned) {
        ArrayList<RelicDef> pool = new ArrayList<>();
        int totalWeight = 0;
        for (RelicDef relic : RELICS.values()) {
            if (relic.rarity == Rarity.BASIC) continue;
            if (owned != null && owned.contains(relic.id)) continue;
            pool.add(relic);
            totalWeight += weight(relic.rarity);
        }
        if (pool.isEmpty()) return null;
        Random r = rng == null ? new Random() : rng;
        int pick = r.nextInt(totalWeight);
        for (RelicDef relic : pool) {
            pick -= weight(relic.rarity);
            if (pick < 0) return relic;
        }
        return pool.get(pool.size() - 1);
    }

    private static int weight(Rarity rarity) {
        if (rarity == Rarity.RARE) return 1;
        if (rarity == Rarity.UNCOMMON) return 3;
        return 6;
    }

    public static List<String> starterRelics() {
        return new ArrayList<>(Arrays.asList("burning_blood"));
    }
}
