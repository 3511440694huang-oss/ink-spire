package com.inkspire.rogue.content;

import com.inkspire.rogue.model.CardDef;
import com.inkspire.rogue.model.CardInstance;
import com.inkspire.rogue.model.CardType;
import com.inkspire.rogue.model.Effect;
import com.inkspire.rogue.model.Rarity;
import com.inkspire.rogue.model.Status;
import com.inkspire.rogue.model.Target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 《墨塔》卡牌内容库（铁甲战士流派）。
 *
 * 只存基础 CardDef，升级形态由 CardDef.upgraded() 依据 plus* 字段生成。
 * 关键词：消耗 = exhaust，虚无 = ethereal，固有 = innate，保留 = retain。
 * 描述里的数值必须和 effects 完全一致，不写引擎没有的机制。
 */
public final class CardLibrary {

    private CardLibrary() {
    }

    /* ---------------- 池 ---------------- */

    private static final List<CardDef> IRON = buildIron();
    private static final List<CardDef> SILK = CardsSilk.build();
    private static final List<CardDef> ASH = CardsAsh.build();
    private static final List<CardDef> SPECIALS = buildSpecials();

    private static final List<CardDef> ALL;
    private static final Map<String, CardDef> BY_ID = new HashMap<>();
    private static final Map<String, List<CardDef>> POOLS = new HashMap<>();
    private static final Map<String, List<CardDef>> STARTERS = new HashMap<>();
    private static final Map<String, List<CardDef>> RARITY_INDEX = new HashMap<>();
    private static final List<CardDef> CURSES = new ArrayList<>();
    private static final List<CardDef> STATUS_CARDS = new ArrayList<>();

    static {
        List<CardDef> all = new ArrayList<>();
        all.addAll(IRON);
        all.addAll(SILK);
        all.addAll(ASH);
        all.addAll(SPECIALS);
        ALL = Collections.unmodifiableList(all);
        for (CardDef d : ALL) BY_ID.put(d.id, d);

        POOLS.put("iron", buildRewardPool(IRON));
        POOLS.put("silk", buildRewardPool(SILK));
        POOLS.put("ash", buildRewardPool(ASH));

        // 稀有度索引（每个角色各自一份，抽不到就退回整个池）
        for (Map.Entry<String, List<CardDef>> e : POOLS.entrySet()) {
            String key = e.getKey();
            for (Rarity r : Rarity.values()) {
                ArrayList<CardDef> sub = new ArrayList<>();
                for (CardDef d : e.getValue()) if (d.rarity == r) sub.add(d);
                if (!sub.isEmpty()) RARITY_INDEX.put(key + "|" + r.name(), Collections.unmodifiableList(sub));
            }
        }

        for (CardDef d : SPECIALS) {
            if (d.type == CardType.CURSE) CURSES.add(d);
            else if (d.type == CardType.STATUS) STATUS_CARDS.add(d);
        }

        STARTERS.put("iron", starterOf("iron", "strike", "defend", "bash", 5, 4, 1, 0));
        STARTERS.put("silk", starterOf("silk", "silk_strike", "silk_defend", "silk_signature", 4, 4, 2, 0));
        STARTERS.put("ash", starterOf("ash", "ash_strike", "ash_defend", "ash_signature", 5, 4, 1, 0));
    }

    public static List<CardDef> all() {
        return ALL;
    }

    public static CardDef get(String id) {
        if (id == null) return null;
        return BY_ID.get(id);
    }

    public static List<CardInstance> starterDeck() {
        return starterDeck("iron");
    }

    /** 某个角色的初始牌组。 */
    public static List<CardInstance> starterDeck(String characterId) {
        ArrayList<CardInstance> out = new ArrayList<>();
        List<CardDef> defs = STARTERS.get(characterId);
        if (defs == null || defs.size() < 5) defs = STARTERS.get("iron");
        if (defs == null) return out;
        for (CardDef d : defs) out.add(new CardInstance(d));
        return out;
    }

    /** 某个角色的可奖励卡池：排除 BASIC / SPECIAL（诅咒、状态）。 */
    public static List<CardDef> poolOf(String characterId) {
        List<CardDef> p = POOLS.get(characterId);
        if (p == null || p.isEmpty()) p = POOLS.get("iron");
        return p == null ? Collections.<CardDef>emptyList() : p;
    }

    public static List<CardDef> rewardPool() { return poolOf("iron"); }

    public static List<CardDef> rewardPool(String characterId) { return poolOf(characterId); }

    public static List<CardDef> curses() { return new ArrayList<>(CURSES); }

    public static List<CardDef> statusCards() { return new ArrayList<>(STATUS_CARDS); }

    public static CardDef randomCurse(Random rng) {
        if (CURSES.isEmpty()) return null;
        return CURSES.get(rng.nextInt(CURSES.size()));
    }

    public static CardDef randomStatusCard(Random rng) {
        if (STATUS_CARDS.isEmpty()) return null;
        return STATUS_CARDS.get(rng.nextInt(STATUS_CARDS.size()));
    }

    /** Distinct card choices for a combat reward. */
    public static List<CardDef> rewardChoices(Random rng, int count, String characterId) {
        ArrayList<CardDef> out = new ArrayList<>();
        int guard = 0;
        while (out.size() < count && guard++ < 200) {
            CardDef c = randomReward(rng, characterId);
            if (c == null) break;
            boolean seen = false;
            for (CardDef e : out) if (e.id.equals(c.id)) { seen = true; break; }
            if (!seen) out.add(c);
        }
        return out;
    }

    public static List<CardDef> rewardChoices(Random rng, int count) {
        return rewardChoices(rng, count, "iron");
    }

    /** 普通 60% / 罕见 30% / 稀有 10%。 */
    public static CardDef randomReward(Random rng) { return randomReward(rng, "iron"); }

    public static CardDef randomReward(Random rng, String characterId) {
        int roll = rng.nextInt(100);
        Rarity want = roll < 60 ? Rarity.COMMON : roll < 90 ? Rarity.UNCOMMON : Rarity.RARE;
        List<CardDef> pool = RARITY_INDEX.get(characterId + "|" + want.name());
        if (pool == null || pool.isEmpty()) pool = poolOf(characterId);
        if (pool == null || pool.isEmpty()) return null;
        return pool.get(rng.nextInt(pool.size()));
    }

    /** 指定稀有度（抽不到就退回同角色的普通池）。 */
    public static CardDef randomOfRarity(Random rng, String characterId, Rarity rarity) {
        List<CardDef> pool = RARITY_INDEX.get(characterId + "|" + rarity.name());
        if (pool == null || pool.isEmpty()) pool = poolOf(characterId);
        if (pool == null || pool.isEmpty()) return null;
        return pool.get(rng.nextInt(pool.size()));
    }

    /** 商店货架：不重复地取 count 张奖励卡。 */
    public static List<CardDef> shopPool(Random rng, int count, String characterId) {
        List<CardDef> pool = new ArrayList<>(poolOf(characterId));
        Collections.shuffle(pool, rng);
        int n = Math.min(Math.max(0, count), pool.size());
        return Collections.unmodifiableList(new ArrayList<>(pool.subList(0, n)));
    }

    public static List<CardDef> shopPool(Random rng, int count) {
        return shopPool(rng, count, "iron");
    }

    /* ---------------- 内部工具 ---------------- */

    private static void addCopies(List<CardInstance> deck, CardDef def, int count) {
        if (def == null) return;
        for (int i = 0; i < count; i++) deck.add(new CardInstance(def));
    }

    private static List<CardDef> starterOf(String charKey, String strikeId, String defendId,
                                           String signatureId, int strikes, int defends,
                                           int signatures, int curses) {
        ArrayList<CardDef> out = new ArrayList<>();
        addDefCopies(out, strikeId, strikes);
        addDefCopies(out, defendId, defends);
        addDefCopies(out, signatureId, signatures);
        for (int i = 0; i < curses; i++) {
            CardDef cu = BY_ID.get("curse_dregs");
            if (cu != null) out.add(cu);
        }
        if (out.size() < 5) {
            // 兜底：角色卡池缺牌时退回铁刻的初始牌
            addDefCopies(out, "strike", 5);
            addDefCopies(out, "defend", 4);
            addDefCopies(out, "bash", 1);
        }
        return Collections.unmodifiableList(out);
    }

    private static void addDefCopies(List<CardDef> out, String id, int count) {
        CardDef def = BY_ID.get(id);
        if (def == null) return;
        for (int i = 0; i < count; i++) out.add(def);
    }

    private static List<CardDef> buildRewardPool(List<CardDef> source) {
        ArrayList<CardDef> out = new ArrayList<>();
        for (CardDef def : source) {
            if (def.rarity != Rarity.BASIC && def.rarity != Rarity.SPECIAL) out.add(def);
        }
        return Collections.unmodifiableList(out);
    }

    private static CardDef.Builder attack(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.ATTACK, rarity, Target.ENEMY);
    }

    private static CardDef.Builder attackAll(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.ATTACK, rarity, Target.ALL_ENEMIES);
    }

    private static CardDef.Builder skill(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.SKILL, rarity, Target.SELF);
    }

    private static CardDef.Builder skillOnEnemy(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.SKILL, rarity, Target.ENEMY);
    }

    private static CardDef.Builder skillOnAll(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.SKILL, rarity, Target.ALL_ENEMIES);
    }

    private static CardDef.Builder power(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.POWER, rarity, Target.SELF);
    }

    private static CardDef.Builder powerOnEnemy(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.POWER, rarity, Target.ENEMY);
    }

    /* ---------------- 内容 ---------------- */

    private static List<CardDef> buildIron() {
        List<CardDef> list = new ArrayList<CardDef>();

        /* ---- 初始牌 ---- */

        list.add(attack("strike", "打击", 1, Rarity.BASIC)
                .description("造成6点伤害。")
                .flavor("最薄的一页，也够割开喉咙。")
                .effects(Effect.damage(6))
                .plus("打击+", "造成9点伤害。", 1, Effect.damage(9))
                .build());

        list.add(skill("defend", "防御", 1, Rarity.BASIC)
                .description("获得5点格挡。")
                .flavor("把一页纸竖起来，就算作墙。")
                .effects(Effect.block(5))
                .plus("防御+", "获得8点格挡。", 1, Effect.block(8))
                .build());

        list.add(attack("bash", "重击", 2, Rarity.BASIC)
                .description("造成8点伤害，使目标获得2层易伤。")
                .flavor("书脊砸下时，字都缩回行间。")
                .effects(Effect.damage(8), Effect.apply(Status.VULNERABLE, 2))
                .plus("重击+", "造成10点伤害，使目标获得3层易伤。", 2,
                        Effect.damage(10), Effect.apply(Status.VULNERABLE, 3))
                .build());

        /* ---- 普通 · 攻击 ---- */

        list.add(attackAll("cleave", "顺劈斩", 1, Rarity.COMMON)
                .description("对所有敌人造成8点伤害。")
                .flavor("一刀横过，排字架齐齐矮了半寸。")
                .effects(Effect.damageAll(8))
                .plus("顺劈斩+", "对所有敌人造成11点伤害。", 1, Effect.damageAll(11))
                .build());

        list.add(attack("iron_wave", "铁斩波", 1, Rarity.COMMON)
                .description("造成5点伤害，获得5点格挡。")
                .flavor("攻与守，原是纸上同一条墨线。")
                .effects(Effect.damage(5), Effect.block(5))
                .plus("铁斩波+", "造成7点伤害，获得7点格挡。", 1,
                        Effect.damage(7), Effect.block(7))
                .build());

        list.add(attack("pommel_strike", "剑柄打击", 1, Rarity.COMMON)
                .description("造成9点伤害，抽1张牌。")
                .flavor("剑柄磕在书脊上，掉出两页旧事。")
                .effects(Effect.damage(9), Effect.draw(1))
                .plus("剑柄打击+", "造成10点伤害，抽2张牌。", 1,
                        Effect.damage(10), Effect.draw(2))
                .build());

        list.add(attack("twin_strike", "双重打击", 1, Rarity.COMMON)
                .description("造成2次5点伤害。")
                .flavor("第一笔是问，第二笔是答。")
                .effects(Effect.damage(5, 2))
                .plus("双重打击+", "造成2次7点伤害。", 1, Effect.damage(7, 2))
                .build());

        list.add(attack("sword_boomerang", "飞剑", 1, Rarity.COMMON)
                .description("对随机敌人造成3次3点伤害。")
                .flavor("剑出鞘时不认人，只认风向。")
                .effects(Effect.damageRandom(3, 3))
                .plus("飞剑+", "对随机敌人造成4次3点伤害。", 1, Effect.damageRandom(3, 4))
                .build());

        list.add(attackAll("thunderclap", "闪电霹雳", 1, Rarity.COMMON)
                .description("对所有敌人造成4点伤害，使所有敌人获得1层易伤。")
                .flavor("墨迹未干，雷已落在页脚。")
                .effects(Effect.damageAll(4), Effect.applyAll(Status.VULNERABLE, 1))
                .plus("闪电霹雳+", "对所有敌人造成7点伤害，使所有敌人获得1层易伤。", 1,
                        Effect.damageAll(7), Effect.applyAll(Status.VULNERABLE, 1))
                .build());

        list.add(attack("clothesline", "晾衣绳", 2, Rarity.COMMON)
                .description("造成12点伤害，使目标获得2层虚弱。")
                .flavor("晾在塔檐的旧袍，还滴着墨。")
                .effects(Effect.damage(12), Effect.apply(Status.WEAK, 2))
                .plus("晾衣绳+", "造成14点伤害，使目标获得3层虚弱。", 2,
                        Effect.damage(14), Effect.apply(Status.WEAK, 3))
                .build());

        /* ---- 罕见 · 攻击 ---- */

        list.add(attack("heavy_blade", "重刃", 2, Rarity.UNCOMMON)
                .description("造成14点伤害。力量对本牌的效果变为3倍。")
                .flavor("力气在刀背上，写得很慢。")
                .effects(Effect.heavy(14, 3))
                .plus("重刃+", "造成14点伤害。力量对本牌的效果变为5倍。", 2, Effect.heavy(14, 5))
                .build());

        list.add(attack("body_slam", "全身撞击", 1, Rarity.UNCOMMON)
                .description("造成等同于你当前格挡的伤害。")
                .flavor("把自己当成最后一页砸下去。")
                .effects(Effect.bodySlam())
                .plus("全身撞击+", "造成等同于你当前格挡的伤害。", 0, Effect.bodySlam())
                .build());

        list.add(attackAll("whirlwind", "旋风斩", 0, Rarity.UNCOMMON)
                .description("消耗你所有能量。对所有敌人造成X次5点伤害，X为消耗的能量。")
                .flavor("转起来时，整座塔跟着拧。")
                .xCost()
                .effects(Effect.damageX(5))
                .plus("旋风斩+", "消耗你所有能量。对所有敌人造成X次7点伤害，X为消耗的能量。", 0,
                        Effect.damageX(7))
                .build());

        list.add(attack("uppercut", "上勾拳", 2, Rarity.UNCOMMON)
                .description("造成13点伤害，使目标获得1层易伤和1层虚弱。")
                .flavor("下颌抬起的一瞬，他读完了自己的结局。")
                .effects(Effect.damage(13), Effect.apply(Status.VULNERABLE, 1), Effect.apply(Status.WEAK, 1))
                .plus("上勾拳+", "造成13点伤害，使目标获得2层易伤和2层虚弱。", 2,
                        Effect.damage(13), Effect.apply(Status.VULNERABLE, 2), Effect.apply(Status.WEAK, 2))
                .build());

        /* ---- 稀有 · 攻击 ---- */

        list.add(attack("bludgeon", "重锤", 3, Rarity.RARE)
                .description("造成32点伤害。")
                .flavor("锤落之前，整座塔安静了一息。")
                .effects(Effect.damage(32))
                .plus("重锤+", "造成42点伤害。", 3, Effect.damage(42))
                .build());

        list.add(attackAll("reaper", "收割", 2, Rarity.RARE)
                .description("对所有敌人造成4点伤害。回复等同于未被格挡伤害的生命。消耗。")
                .flavor("镰刀过处，墨回灌进执笔人的腕。")
                .exhaust()
                .plusExhaust()
                .healUnblocked()
                .effects(Effect.damageAll(4))
                .plus("收割+", "对所有敌人造成5点伤害。回复等同于未被格挡伤害的生命。消耗。", 2,
                        Effect.damageAll(5))
                .build());

        list.add(attack("feed", "狂宴", 1, Rarity.RARE)
                .description("造成10点伤害。若因此击杀敌人，最大生命+3。消耗。")
                .flavor("吞下的字，会在骨头上长出新芽。")
                .exhaust()
                .plusExhaust()
                .onKillMaxHp(3)
                .effects(Effect.damage(10))
                .plus("狂宴+", "造成12点伤害。若因此击杀敌人，最大生命+3。消耗。", 1,
                        Effect.damage(12))
                .build());

        /* ---- 普通 · 技能 ---- */

        list.add(skill("shrug_it_off", "耸肩", 1, Rarity.COMMON)
                .description("获得8点格挡，抽1张牌。")
                .flavor("肩一沉，千页的重量便滑落。")
                .effects(Effect.block(8), Effect.draw(1))
                .plus("耸肩+", "获得11点格挡，抽1张牌。", 1, Effect.block(11), Effect.draw(1))
                .build());

        list.add(skill("true_grit", "坚毅", 1, Rarity.COMMON)
                .description("获得7点格挡，随机消耗1张手牌。")
                .flavor("咬住这一页，别让它翻过去。")
                .effects(Effect.block(7), Effect.exhaustRandom(1))
                .plus("坚毅+", "获得9点格挡，随机消耗1张手牌。", 1,
                        Effect.block(9), Effect.exhaustRandom(1))
                .build());

        list.add(skill("ghostly_armor", "幽灵铠甲", 1, Rarity.COMMON)
                .description("获得10点格挡。虚无。")
                .flavor("铠甲是褪色的字迹，挡得住风。")
                .ethereal()
                .effects(Effect.block(10))
                .plus("幽灵铠甲+", "获得13点格挡。虚无。", 1, Effect.block(13))
                .build());

        list.add(skill("flex", "屈伸", 0, Rarity.COMMON)
                .description("获得2点力量。消耗。")
                .flavor("把骨节折起来，力就多出一截。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.power(Status.STRENGTH, 2))
                .plus("屈伸+", "获得3点力量。消耗。", 0, Effect.power(Status.STRENGTH, 3))
                .build());

        /* ---- 稀有 · 技能 ---- */

        list.add(skill("impervious", "坚不可摧", 2, Rarity.RARE)
                .description("获得30点格挡。消耗。")
                .flavor("这一页不写别的，只写不肯。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.block(30))
                .plus("坚不可摧+", "获得40点格挡。消耗。", 2, Effect.block(40))
                .build());

        list.add(skill("limit_break", "极限突破", 1, Rarity.RARE)
                .description("使你的力量翻倍。")
                .flavor("界限原来只是纸糊的一道折痕。")
                .effects(Effect.doubleStrength())
                .plus("极限突破+", "使你的力量翻倍。", 0, Effect.doubleStrength())
                .build());

        list.add(skill("offering", "献祭", 0, Rarity.RARE)
                .description("失去6点生命，获得2点能量，抽3张牌。消耗。")
                .flavor("烧掉一页，换三行还没写的字。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.loseHp(6), Effect.energy(2), Effect.draw(3))
                .plus("献祭+", "失去6点生命，获得2点能量，抽5张牌。消耗。", 0,
                        Effect.loseHp(6), Effect.energy(2), Effect.draw(5))
                .build());

        list.add(skill("entrench", "巩固", 2, Rarity.RARE)
                .description("获得15点格挡。")
                .flavor("一层压一层，像没人翻过的书。")
                .effects(Effect.block(15))
                .plus("巩固+", "获得22点格挡。", 2, Effect.block(22))
                .build());

        /* ---- 罕见 · 技能 ---- */

        list.add(skill("second_wind", "重振精神", 1, Rarity.UNCOMMON)
                .description("消耗你手中所有非攻击牌，每消耗1张获得5点格挡。")
                .flavor("把没用的页撕了，风就顺了。")
                .effects(Effect.exhaustNonAttackBlock(5))
                .plus("重振精神+", "消耗你手中所有非攻击牌，每消耗1张获得7点格挡。", 1,
                        Effect.exhaustNonAttackBlock(7))
                .build());

        list.add(skill("bloodletting", "放血", 0, Rarity.UNCOMMON)
                .description("失去3点生命，获得2点能量。")
                .flavor("墨是黑的，血也是，都往低处流。")
                .effects(Effect.loseHp(3), Effect.energy(2))
                .plus("放血+", "失去3点生命，获得3点能量。", 0,
                        Effect.loseHp(3), Effect.energy(3))
                .build());

        list.add(skill("battle_trance", "战吼", 1, Rarity.UNCOMMON)
                .description("抽3张牌。本回合不能再抽牌。")
                .flavor("一声吼把满架的纸震落了几张。")
                .noDraw()
                .plusNoDraw()
                .effects(Effect.draw(3))
                .plus("战吼+", "抽4张牌。本回合不能再抽牌。", 1, Effect.draw(4))
                .build());

        list.add(skillOnAll("shockwave", "震地", 2, Rarity.UNCOMMON)
                .description("使所有敌人获得3层虚弱和3层易伤。消耗。")
                .flavor("地一动，所有字都往同一侧倒。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.applyAll(Status.WEAK, 3), Effect.applyAll(Status.VULNERABLE, 3))
                .plus("震地+", "使所有敌人获得5层虚弱和5层易伤。消耗。", 2,
                        Effect.applyAll(Status.WEAK, 5), Effect.applyAll(Status.VULNERABLE, 5))
                .build());

        list.add(skillOnEnemy("disarm", "缴械", 1, Rarity.UNCOMMON)
                .description("使目标失去2点力量。消耗。")
                .flavor("夺走刀的那一刻，比挥刀更响。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.apply(Status.STRENGTH, -2))
                .plus("缴械+", "使目标失去3点力量。消耗。", 1, Effect.apply(Status.STRENGTH, -3))
                .build());

        list.add(skill("rage", "盛怒", 0, Rarity.UNCOMMON)
                .description("获得6点格挡。消耗。")
                .flavor("怒到极处，反而像一页压平的纸。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.block(6))
                .plus("盛怒+", "获得9点格挡。消耗。", 0, Effect.block(9))
                .build());

        list.add(skill("double_tap", "双发", 1, Rarity.UNCOMMON)
                .description("本回合下一张攻击牌打出两次。")
                .flavor("同一句话，说了两遍就成了誓。")
                .effects(Effect.power(Status.DOUBLE_TAP, 1))
                .plus("双发+", "本回合下一张攻击牌打出两次。", 0,
                        Effect.power(Status.DOUBLE_TAP, 1))
                .build());

        list.add(skill("seeing_red", "见红", 1, Rarity.UNCOMMON)
                .description("获得2点能量。消耗。")
                .flavor("眼睛红了，塔里的灯反而暗了。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.energy(2))
                .plus("见红+", "获得2点能量。消耗。", 0, Effect.energy(2))
                .build());

        /* ---- 能力 ---- */

        list.add(power("inflame", "燃烧", 1, Rarity.UNCOMMON)
                .description("获得2点力量。")
                .flavor("火不识字，它只认得纸的边角。")
                .effects(Effect.power(Status.STRENGTH, 2))
                .plus("燃烧+", "获得3点力量。", 1, Effect.power(Status.STRENGTH, 3))
                .build());

        list.add(power("metallicize", "金属化", 1, Rarity.UNCOMMON)
                .description("每回合结束时获得3点格挡。")
                .flavor("皮肤下渗出薄薄一层铅灰。")
                .effects(Effect.power(Status.METALLICIZE, 3))
                .plus("金属化+", "每回合结束时获得4点格挡。", 1, Effect.power(Status.METALLICIZE, 4))
                .build());

        list.add(power("feel_no_pain", "无惧", 1, Rarity.UNCOMMON)
                .description("每当你消耗一张牌，获得3点格挡。")
                .flavor("撕掉的那页，替你挡了一下。")
                .effects(Effect.power(Status.FEEL_NO_PAIN, 3))
                .plus("无惧+", "每当你消耗一张牌，获得4点格挡。", 1, Effect.power(Status.FEEL_NO_PAIN, 4))
                .build());

        list.add(power("dark_embrace", "黑暗之拥", 2, Rarity.UNCOMMON)
                .description("每当你消耗一张牌，抽1张牌。")
                .flavor("暗处有手，把烧尽的字捡回来。")
                .effects(Effect.power(Status.DARK_EMBRACE, 1))
                .plus("黑暗之拥+", "每当你消耗一张牌，抽1张牌。", 1,
                        Effect.power(Status.DARK_EMBRACE, 1))
                .build());

        list.add(power("evolve", "进化", 1, Rarity.UNCOMMON)
                .description("每当你抽到状态牌，抽1张牌。")
                .flavor("坏掉的字，也能长成新的偏旁。")
                .effects(Effect.power(Status.EVOLVE, 1))
                .plus("进化+", "每当你抽到状态牌，抽2张牌。", 1, Effect.power(Status.EVOLVE, 2))
                .build());

        list.add(power("rupture", "撕裂", 1, Rarity.UNCOMMON)
                .description("每当你失去生命，获得1点力量。")
                .flavor("伤口张开的地方，力量往里灌。")
                .effects(Effect.power(Status.RUPTURE, 1))
                .plus("撕裂+", "每当你失去生命，获得2点力量。", 1, Effect.power(Status.RUPTURE, 2))
                .build());

        list.add(power("flame_barrier", "火焰屏障", 2, Rarity.UNCOMMON)
                .description("获得4层火焰屏障：受到攻击时对攻击者造成4点伤害。")
                .flavor("门框上还留着上一场火的牙印。")
                .effects(Effect.power(Status.FLAME_BARRIER, 4))
                .plus("火焰屏障+", "获得6层火焰屏障：受到攻击时对攻击者造成6点伤害。", 2,
                        Effect.power(Status.FLAME_BARRIER, 6))
                .build());

        list.add(power("thorns", "荆棘之甲", 1, Rarity.UNCOMMON)
                .description("获得3层荆棘：受到攻击时对攻击者造成3点伤害。")
                .flavor("铠甲内侧，密密麻麻写着刺。")
                .effects(Effect.power(Status.THORNS, 3))
                .plus("荆棘之甲+", "获得4层荆棘：受到攻击时对攻击者造成4点伤害。", 1,
                        Effect.power(Status.THORNS, 4))
                .build());

        list.add(powerOnEnemy("spot_weakness", "力量爆发", 1, Rarity.UNCOMMON)
                .description("获得2点力量，使目标获得1层易伤。")
                .flavor("人人都有裂开的一处，只是他先看见。")
                .effects(Effect.power(Status.STRENGTH, 2), Effect.apply(Status.VULNERABLE, 1))
                .plus("力量爆发+", "获得3点力量，使目标获得2层易伤。", 1,
                        Effect.power(Status.STRENGTH, 3), Effect.apply(Status.VULNERABLE, 2))
                .build());

        list.add(power("demon_form", "恶魔形态", 3, Rarity.RARE)
                .description("每回合开始时获得2点力量。")
                .flavor("名字被涂掉的地方，长出了角。")
                .effects(Effect.power(Status.DEMON_FORM, 2))
                .plus("恶魔形态+", "每回合开始时获得3点力量。", 3, Effect.power(Status.DEMON_FORM, 3))
                .build());

        list.add(power("juggernaut", "势不可挡", 2, Rarity.RARE)
                .description("每当你获得格挡，对随机敌人造成5点伤害。")
                .flavor("挡下来的每一寸，都还了回去。")
                .effects(Effect.power(Status.JUGGERNAUT, 5))
                .plus("势不可挡+", "每当你获得格挡，对随机敌人造成7点伤害。", 2,
                        Effect.power(Status.JUGGERNAUT, 7))
                .build());

        list.add(power("barricade", "壁垒", 3, Rarity.RARE)
                .description("格挡不再于回合开始时消失。")
                .flavor("墙不拆，只一层层往上砌。")
                .effects(Effect.power(Status.BARRICADE, 1))
                .plus("壁垒+", "格挡不再于回合开始时消失。", 2, Effect.power(Status.BARRICADE, 1))
                .build());

        list.add(power("corruption", "腐化", 3, Rarity.RARE)
                .description("技能牌的费用变为0，且打出后被消耗。")
                .flavor("书页烂透了，翻起来反而顺手。")
                .effects(Effect.power(Status.CORRUPTION, 1))
                .plus("腐化+", "技能牌的费用变为0，且打出后被消耗。", 2,
                        Effect.power(Status.CORRUPTION, 1))
                .build());

        list.add(power("berserk", "狂暴", 1, Rarity.RARE)
                .description("每回合开始时获得1点能量。")
                .flavor("他把最后一点理智撕下来点了火。")
                .effects(Effect.power(Status.BERSERK, 1))
                .plus("狂暴+", "每回合开始时获得1点能量。", 0, Effect.power(Status.BERSERK, 1))
                .build());

        list.add(power("brutality", "残暴", 1, Rarity.RARE)
                .description("每回合开始时失去1点生命，抽1张牌。")
                .flavor("每天割一点，字就能多写一行。")
                .effects(Effect.power(Status.BRUTALITY, 1))
                .plus("残暴+", "每回合开始时失去1点生命，抽1张牌。", 0,
                        Effect.power(Status.BRUTALITY, 1))
                .build());

        return Collections.unmodifiableList(list);
    }

    /** 诅咒与状态牌：不可打出，只会占住手牌。 */
    private static List<CardDef> buildSpecials() {
        List<CardDef> list = new ArrayList<CardDef>();

        list.add(CardDef.builder("curse_dregs", "残墨", 0, CardType.CURSE, Rarity.SPECIAL, Target.NONE)
                .description("无法打出。")
                .flavor("干在砚台底的那一层，刮不下来了。")
                .build());

        list.add(CardDef.builder("curse_regret", "悔恨", 0, CardType.CURSE, Rarity.SPECIAL, Target.NONE)
                .description("无法打出。回合结束时若在手牌，失去1点生命。")
                .flavor("你本可以少写一句的。")
                .handEndTurnLoseHp(1)
                .build());

        list.add(CardDef.builder("curse_decay", "腐朽", 0, CardType.CURSE, Rarity.SPECIAL, Target.NONE)
                .description("无法打出。回合结束时若在手牌，失去2点生命。")
                .flavor("纸在袖子里慢慢烂熟。")
                .handEndTurnLoseHp(2)
                .build());

        list.add(CardDef.builder("curse_doubt", "疑虑", 0, CardType.CURSE, Rarity.SPECIAL, Target.NONE)
                .description("无法打出。回合开始时若在手牌，获得1层虚弱。")
                .flavor("写到一半，忽然不知道写给谁。")
                .handTurnStartWeak(1)
                .build());

        list.add(CardDef.builder("curse_bind", "缠结", 0, CardType.CURSE, Rarity.SPECIAL, Target.NONE)
                .description("无法打出。回合结束时若在手牌，失去1点生命。")
                .flavor("线头找不到，越拉越紧。")
                .handEndTurnLoseHp(1)
                .build());

        list.add(CardDef.builder("status_wound", "伤口", 0, CardType.STATUS, Rarity.SPECIAL, Target.NONE)
                .description("无法打出。")
                .flavor("一页被撕开的地方，边缘还很新。")
                .build());

        list.add(CardDef.builder("status_burn", "灼烧", 0, CardType.STATUS, Rarity.SPECIAL, Target.NONE)
                .description("无法打出。回合结束时若在手牌，失去2点生命。")
                .flavor("余火沿着行距爬。")
                .handEndTurnLoseHp(2)
                .build());

        list.add(CardDef.builder("status_daze", "眩晕", 0, CardType.STATUS, Rarity.SPECIAL, Target.NONE)
                .description("无法打出。")
                .flavor("字忽然全都站错了边。")
                .build());

        list.add(CardDef.builder("status_slimed", "黏液", 0, CardType.STATUS, Rarity.SPECIAL, Target.NONE)
                .description("无法打出。")
                .flavor("冰凉的一团，还带着上一页的味道。")
                .build());

        return Collections.unmodifiableList(list);
    }
}
