package com.inkspire.rogue.content;

import com.inkspire.rogue.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 《墨塔》卡牌内容库（角色「灰烬」卡池）。
 *
 * 只存基础 CardDef，升级形态由 CardDef.upgraded() 依据 plus* 字段生成。
 * 关键词：消耗 = exhaust，虚无 = ethereal，固有 = innate，保留 = retain。
 * 描述里的数值必须和 effects 完全一致，不写引擎没有的机制。
 *
 * 设计定位：生命即货币。自伤换能量、换抽牌、换爆发；
 * 用 RUPTURE / 回复 / 最大生命把血债赚回来。
 */
public final class CardsAsh {

    private CardsAsh() {
    }

    /* ---------------- 构造工具 ---------------- */

    private static CardDef.Builder attack(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.ATTACK, rarity, Target.ENEMY);
    }

    private static CardDef.Builder attackAll(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.ATTACK, rarity, Target.ALL_ENEMIES);
    }

    private static CardDef.Builder skill(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.SKILL, rarity, Target.SELF);
    }

    private static CardDef.Builder power(String id, String name, int cost, Rarity rarity) {
        return CardDef.builder(id, name, cost, CardType.POWER, rarity, Target.SELF);
    }

    /* ---------------- 池 ---------------- */

    /**
     * 30 张：2 初始 + 1 签名 + 12 普通 + 10 罕见 + 5 稀有。
     * 类型：攻击 13 / 技能 12 / 能力 5。
     */
    public static List<CardDef> build() {
        List<CardDef> list = new ArrayList<CardDef>(32);

        /* ---- 初始牌 ---- */

        list.add(attack("ash_strike", "灰烬打击", 1, Rarity.BASIC)
                .description("造成7点伤害。")
                .flavor("第一下是灰，第二下才是火。")
                .effects(Effect.damage(7))
                .plus("灰烬打击+", "造成10点伤害。", 1, Effect.damage(10))
                .build());

        list.add(skill("ash_defend", "掩灰", 1, Rarity.BASIC)
                .description("获得5点格挡。")
                .flavor("把烧剩的墙根挡在胸前，还烫着。")
                .effects(Effect.block(5))
                .plus("掩灰+", "获得8点格挡。", 1, Effect.block(8))
                .build());

        list.add(attack("ash_signature", "以血为墨", 1, Rarity.BASIC)
                .description("失去2点生命，造成8点伤害。")
                .flavor("血一落纸就黑了，字因此有了重量。")
                .effects(Effect.loseHp(2), Effect.damage(8))
                .plus("以血为墨+", "失去2点生命，造成11点伤害。", 1,
                        Effect.loseHp(2), Effect.damage(11))
                .build());

        /* ---- 普通 · 攻击 ---- */

        list.add(attack("ash_claw", "灰爪", 1, Rarity.COMMON)
                .description("造成2次4点伤害。")
                .flavor("抓痕干得很快，血还是烫的。")
                .effects(Effect.damage(4, 2))
                .plus("灰爪+", "造成2次5点伤害。", 1, Effect.damage(5, 2))
                .build());

        list.add(attackAll("ash_scorch", "焚手", 1, Rarity.COMMON)
                .description("失去2点生命，对所有敌人造成7点伤害。")
                .flavor("手掌先烧起来，然后才是别人的脸。")
                .effects(Effect.loseHp(2), Effect.damageAll(7))
                .plus("焚手+", "失去2点生命，对所有敌人造成9点伤害。", 1,
                        Effect.loseHp(2), Effect.damageAll(9))
                .build());

        list.add(attack("ash_rip", "划开", 1, Rarity.COMMON)
                .description("造成6点伤害，使目标获得1层易伤。")
                .flavor("伤口是最好的书签，翻到就一定看见。")
                .effects(Effect.damage(6), Effect.apply(Status.VULNERABLE, 1))
                .plus("划开+", "造成8点伤害，使目标获得1层易伤。", 1,
                        Effect.damage(8), Effect.apply(Status.VULNERABLE, 1))
                .build());

        list.add(attack("ash_bone_blade", "骨刃", 2, Rarity.COMMON)
                .description("失去2点生命，造成17点伤害。")
                .flavor("取一根自己的骨头，磨到能割纸。")
                .effects(Effect.loseHp(2), Effect.damage(17))
                .plus("骨刃+", "失去2点生命，造成20点伤害。", 2,
                        Effect.loseHp(2), Effect.damage(20))
                .build());

        list.add(attackAll("ash_ashfall", "落灰", 1, Rarity.COMMON)
                .description("对所有敌人造成4点伤害，使所有敌人获得1层虚弱。")
                .flavor("塔顶的灰落下来，谁也躲不开。")
                .effects(Effect.damageAll(4), Effect.applyAll(Status.WEAK, 1))
                .plus("落灰+", "对所有敌人造成6点伤害，使所有敌人获得1层虚弱。", 1,
                        Effect.damageAll(6), Effect.applyAll(Status.WEAK, 1))
                .build());

        list.add(attack("ash_cinder_page", "烬页", 1, Rarity.COMMON)
                .description("失去2点生命，造成6点伤害。将本牌的复制加入弃牌堆。")
                .flavor("烧过的页子会自己长回来，像疤。")
                .addCopyToDiscard()
                .effects(Effect.loseHp(2), Effect.damage(6))
                .plus("烬页+", "失去2点生命，造成8点伤害。将本牌的复制加入弃牌堆。", 1,
                        Effect.loseHp(2), Effect.damage(8))
                .build());

        /* ---- 普通 · 技能 ---- */

        list.add(skill("ash_wall", "灰墙", 1, Rarity.COMMON)
                .description("获得8点格挡。")
                .flavor("墙是灰压成的，推一下就掉一层。")
                .effects(Effect.block(8))
                .plus("灰墙+", "获得11点格挡。", 1, Effect.block(11))
                .build());

        list.add(skill("ash_endure", "忍痛", 1, Rarity.COMMON)
                .description("失去3点生命，获得8点格挡，抽1张牌。")
                .flavor("疼的时候咬住的那一页，最结实。")
                .effects(Effect.loseHp(3), Effect.block(8), Effect.draw(1))
                .plus("忍痛+", "失去3点生命，获得11点格挡，抽1张牌。", 1,
                        Effect.loseHp(3), Effect.block(11), Effect.draw(1))
                .build());

        list.add(skill("ash_burn_page", "燃页", 1, Rarity.COMMON)
                .description("随机消耗1张手牌，抽2张牌。")
                .flavor("烧掉的那页，替你翻了后面三页。")
                .effects(Effect.exhaustRandom(1), Effect.draw(2))
                .plus("燃页+", "随机消耗1张手牌，抽3张牌。", 1,
                        Effect.exhaustRandom(1), Effect.draw(3))
                .build());

        list.add(skill("ash_ink_drop", "滴墨", 1, Rarity.COMMON)
                .description("失去3点生命，抽2张牌。")
                .flavor("一滴就够，整行字都往黑里去。")
                .effects(Effect.loseHp(3), Effect.draw(2))
                .plus("滴墨+", "失去3点生命，抽3张牌。", 1,
                        Effect.loseHp(3), Effect.draw(3))
                .build());

        list.add(skill("ash_scour", "焚稿", 1, Rarity.COMMON)
                .description("消耗你手中所有非攻击牌，每消耗1张获得4点格挡。")
                .flavor("写坏的东西，最好的去处是灶膛。")
                .effects(Effect.exhaustNonAttackBlock(4))
                .plus("焚稿+", "消耗你手中所有非攻击牌，每消耗1张获得6点格挡。", 1,
                        Effect.exhaustNonAttackBlock(6))
                .build());

        list.add(skill("ash_tinder", "火种", 0, Rarity.COMMON)
                .description("失去3点生命，获得1点力量，抽1张牌。")
                .flavor("留一点火星在袖口，走哪都烫。")
                .effects(Effect.loseHp(3), Effect.power(Status.STRENGTH, 1), Effect.draw(1))
                .plus("火种+", "失去3点生命，获得2点力量，抽1张牌。", 0,
                        Effect.loseHp(3), Effect.power(Status.STRENGTH, 2), Effect.draw(1))
                .build());

        /* ---- 罕见 · 攻击 ---- */

        list.add(attack("ash_heavy_ember", "重烬", 2, Rarity.UNCOMMON)
                .description("造成14点伤害。力量对本牌的效果变为2倍。")
                .flavor("烬压成块，比铁还沉三分。")
                .effects(Effect.heavy(14, 2))
                .plus("重烬+", "造成16点伤害。力量对本牌的效果变为3倍。", 2,
                        Effect.heavy(16, 3))
                .build());

        list.add(attack("ash_shiver", "抖灰", 1, Rarity.UNCOMMON)
                .description("造成3次3点伤害，随机消耗1张手牌。")
                .flavor("抖一抖，掉下来的不止是灰。")
                .effects(Effect.damage(3, 3), Effect.exhaustRandom(1))
                .plus("抖灰+", "造成3次4点伤害，随机消耗1张手牌。", 1,
                        Effect.damage(4, 3), Effect.exhaustRandom(1))
                .build());

        list.add(attack("ash_drink_blood", "饮血", 2, Rarity.UNCOMMON)
                .description("造成16点伤害。回复等同于未被格挡伤害的生命。")
                .flavor("被烧过的地方，最会吸东西。")
                .healUnblocked()
                .effects(Effect.damage(16))
                .plus("饮血+", "造成19点伤害。回复等同于未被格挡伤害的生命。", 2,
                        Effect.damage(19))
                .build());

        list.add(attack("ash_poison_ink", "毒墨", 1, Rarity.UNCOMMON)
                .description("造成6点伤害，使目标获得3层中毒。")
                .flavor("墨里掺了自己的一点东西。")
                .effects(Effect.damage(6), Effect.apply(Status.POISON, 3))
                .plus("毒墨+", "造成8点伤害，使目标获得4层中毒。", 1,
                        Effect.damage(8), Effect.apply(Status.POISON, 4))
                .build());

        /* ---- 罕见 · 技能 ---- */

        list.add(skill("ash_blood_pull", "血引", 0, Rarity.UNCOMMON)
                .description("失去3点生命，获得2点能量。")
                .flavor("开一道口子，光就照进来了。")
                .effects(Effect.loseHp(3), Effect.energy(2))
                .plus("血引+", "失去3点生命，获得3点能量。", 0,
                        Effect.loseHp(3), Effect.energy(3))
                .build());

        list.add(skill("ash_gray_armor", "灰甲", 2, Rarity.UNCOMMON)
                .description("获得16点格挡。")
                .flavor("烧出来的壳，一敲就是一片响。")
                .effects(Effect.block(16))
                .plus("灰甲+", "获得20点格挡。", 2, Effect.block(20))
                .build());

        list.add(skill("ash_burn_fuel", "烧薪", 1, Rarity.UNCOMMON)
                .description("随机消耗2张手牌，获得2点能量。")
                .flavor("柴不够了，就把抽屉也劈开。")
                .effects(Effect.exhaustRandom(2), Effect.energy(2))
                .plus("烧薪+", "随机消耗1张手牌，获得2点能量。", 1,
                        Effect.exhaustRandom(1), Effect.energy(2))
                .build());

        list.add(skill("ash_scab", "结痂", 1, Rarity.UNCOMMON)
                .description("回复5点生命，抽1张牌。")
                .flavor("血干了成壳，底下的肉在长。")
                .effects(Effect.heal(5), Effect.draw(1))
                .plus("结痂+", "回复7点生命，抽1张牌。", 1, Effect.heal(7), Effect.draw(1))
                .build());

        /* ---- 罕见 · 能力 ---- */

        list.add(power("ash_old_wound", "旧伤", 1, Rarity.UNCOMMON)
                .description("每当你失去生命，获得1点力量。")
                .flavor("每裂开一次，缝上去的线就粗一分。")
                .effects(Effect.power(Status.RUPTURE, 1))
                .plus("旧伤+", "每当你失去生命，获得2点力量。", 1,
                        Effect.power(Status.RUPTURE, 2))
                .build());

        list.add(power("ash_gray_glaze", "灰釉", 1, Rarity.UNCOMMON)
                .description("每回合结束时获得3点格挡。")
                .flavor("烧过一遍的皮，反而更经得住烧。")
                .effects(Effect.power(Status.METALLICIZE, 3))
                .plus("灰釉+", "每回合结束时获得4点格挡。", 1,
                        Effect.power(Status.METALLICIZE, 4))
                .build());

        /* ---- 罕见（补充：血换伤、毒、消耗、燃能） ---- */

        list.add(attack("ash_blood_feast", "血宴", 2, Rarity.UNCOMMON)
                .description("失去3点生命，造成20点伤害。消耗。")
                .flavor("先割自己，再割别人，刀口一直没凉过。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.loseHp(3), Effect.damage(20))
                .plus("血宴+", "失去3点生命，造成25点伤害。消耗。", 2,
                        Effect.loseHp(3), Effect.damage(25))
                .build());

        list.add(attackAll("ash_cinder_trail", "烬迹", 1, Rarity.UNCOMMON)
                .description("对所有敌人造成6点伤害，使所有敌人获得1层易伤。")
                .flavor("走过的路自己烧起来，回头就看不见了。")
                .effects(Effect.damageAll(6), Effect.applyAll(Status.VULNERABLE, 1))
                .plus("烬迹+", "对所有敌人造成8点伤害，使所有敌人获得2层易伤。", 1,
                        Effect.damageAll(8), Effect.applyAll(Status.VULNERABLE, 2))
                .build());

        list.add(attack("ash_grave_hand", "墓手", 2, Rarity.UNCOMMON)
                .description("造成9点伤害，使目标获得2层虚弱。")
                .flavor("土里的手伸出来，只是想再翻一页。")
                .effects(Effect.damage(9), Effect.apply(Status.WEAK, 2))
                .plus("墓手+", "造成12点伤害，使目标获得3层虚弱。", 2,
                        Effect.damage(12), Effect.apply(Status.WEAK, 3))
                .build());

        list.add(skill("ash_poison_mist", "毒雾", 2, Rarity.UNCOMMON)
                .description("使所有敌人获得4层中毒，失去3点生命。")
                .flavor("雾是墨熬的，吸进去的字会烂在肺里。")
                .effects(Effect.applyAll(Status.POISON, 4), Effect.loseHp(3))
                .plus("毒雾+", "使所有敌人获得6层中毒，失去3点生命。", 2,
                        Effect.applyAll(Status.POISON, 6), Effect.loseHp(3))
                .build());

        list.add(skill("ash_ember_shell", "烬壳", 1, Rarity.UNCOMMON)
                .description("获得9点格挡，失去2点生命。")
                .flavor("壳还烫手，但至少挡得住第一下。")
                .effects(Effect.block(9), Effect.loseHp(2))
                .plus("烬壳+", "获得13点格挡，失去2点生命。", 1, Effect.block(13), Effect.loseHp(2))
                .build());

        list.add(skill("ash_fuel_drop", "添薪", 1, Rarity.UNCOMMON)
                .description("获得2点力量，失去4点生命。")
                .flavor("把自己添进去，火才肯再高一点。")
                .effects(Effect.power(Status.STRENGTH, 2), Effect.loseHp(4))
                .plus("添薪+", "获得3点力量，失去4点生命。", 1,
                        Effect.power(Status.STRENGTH, 3), Effect.loseHp(4))
                .build());

        list.add(skill("ash_burn_book", "焚书", 2, Rarity.UNCOMMON)
                .description("随机消耗2张手牌，抽3张牌。")
                .flavor("整本都烧了，灰里的字反而更清楚。")
                .effects(Effect.exhaustRandom(2), Effect.draw(3))
                .plus("焚书+", "随机消耗2张手牌，抽4张牌。", 2,
                        Effect.exhaustRandom(2), Effect.draw(4))
                .build());

        list.add(skill("ash_scar_tissue", "疤痕", 2, Rarity.UNCOMMON)
                .description("回复6点生命，获得6点格挡。")
                .flavor("结痂的地方最硬，也最不听话。")
                .effects(Effect.heal(6), Effect.block(6))
                .plus("疤痕+", "回复9点生命，获得9点格挡。", 2, Effect.heal(9), Effect.block(9))
                .build());

        list.add(attack("ash_ember_dust", "灰烬尘", 0, Rarity.UNCOMMON)
                .description("造成3点伤害，使目标获得1层中毒。消耗。")
                .flavor("吹一口气，整条走廊都在咳。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.damage(3), Effect.apply(Status.POISON, 1))
                .plus("灰烬尘+", "造成5点伤害，使目标获得1层中毒。消耗。", 0,
                        Effect.damage(5), Effect.apply(Status.POISON, 1))
                .build());

        list.add(skill("ash_smoke_veil", "烟幕", 1, Rarity.UNCOMMON)
                .description("获得6点格挡，使所有敌人获得1层虚弱。")
                .flavor("烟一起来，谁也数不清台阶有几级。")
                .effects(Effect.block(6), Effect.applyAll(Status.WEAK, 1))
                .plus("烟幕+", "获得9点格挡，使所有敌人获得2层虚弱。", 1,
                        Effect.block(9), Effect.applyAll(Status.WEAK, 2))
                .build());

        list.add(power("ash_thorn_ash", "灰刺", 1, Rarity.UNCOMMON)
                .description("获得3点荆棘，失去2点生命。")
                .flavor("灰里掺了玻璃渣，抓一把就忘不掉。")
                .effects(Effect.power(Status.THORNS, 3), Effect.loseHp(2))
                .plus("灰刺+", "获得4点荆棘，失去2点生命。", 1,
                        Effect.power(Status.THORNS, 4), Effect.loseHp(2))
                .build());

        /* ---- 稀有 · 攻击 ---- */

        list.add(attack("ash_immolate", "焚尸", 3, Rarity.RARE)
                .description("造成30点伤害。若因此击杀敌人，最大生命+2。")
                .flavor("把自己整个投进去，才烧得干净。")
                .onKillMaxHp(2)
                .effects(Effect.damage(30))
                .plus("焚尸+", "造成38点伤害。若因此击杀敌人，最大生命+2。", 3,
                        Effect.damage(38))
                .build());

        /* ---- 稀有 · 技能 ---- */

        list.add(skill("ash_kindled_heart", "焚心", 1, Rarity.RARE)
                .description("失去6点生命，使你的力量翻倍，抽1张牌。")
                .flavor("心先烧穿，手才敢往下落。")
                .effects(Effect.loseHp(6), Effect.doubleStrength(), Effect.draw(1))
                .plus("焚心+", "失去4点生命，使你的力量翻倍，抽1张牌。", 1,
                        Effect.loseHp(4), Effect.doubleStrength(), Effect.draw(1))
                .build());

        /* ---- 稀有 · 能力 ---- */

        list.add(power("ash_fuel", "薪", 2, Rarity.RARE)
                .description("每回合开始时失去1点生命，抽1张牌，获得1点能量。")
                .flavor("烧掉自己的一角，火就还能续一夜。")
                .effects(Effect.power(Status.BERSERK, 1), Effect.power(Status.BRUTALITY, 1))
                .plus("薪+", "每回合开始时失去1点生命，抽1张牌，获得1点能量。", 1,
                        Effect.power(Status.BERSERK, 1), Effect.power(Status.BRUTALITY, 1))
                .build());

        list.add(power("ash_crown", "灰冠", 2, Rarity.RARE)
                .description("每回合开始时获得1点力量。最大生命+5。")
                .flavor("冠是烧剩下的那一圈灰，很轻。")
                .effects(Effect.power(Status.DEMON_FORM, 1), Effect.maxHp(5))
                .plus("灰冠+", "每回合开始时获得2点力量。最大生命+5。", 2,
                        Effect.power(Status.DEMON_FORM, 2), Effect.maxHp(5))
                .build());

        list.add(power("ash_ember_body", "余烬之躯", 2, Rarity.RARE)
                .description("获得4层荆棘；每回合结束时获得3点格挡。")
                .flavor("烧剩的骨架，碰一下就把人燎了。")
                .effects(Effect.power(Status.THORNS, 4), Effect.power(Status.METALLICIZE, 3))
                .plus("余烬之躯+", "获得6层荆棘；每回合结束时获得4点格挡。", 2,
                        Effect.power(Status.THORNS, 6), Effect.power(Status.METALLICIZE, 4))
                .build());

        list.add(attackAll("ash_inferno", "炼狱", 3, Rarity.RARE)
                .description("对所有敌人造成2次10点伤害，失去6点生命。")
                .flavor("火是从最底下那层开始烧的，往上一直烧到落款。")
                .effects(Effect.damageAll(10, 2), Effect.loseHp(6))
                .plus("炼狱+", "对所有敌人造成2次13点伤害，失去6点生命。", 3,
                        Effect.damageAll(13, 2), Effect.loseHp(6))
                .build());

        list.add(power("ash_demon_heart", "魔心", 3, Rarity.RARE)
                .description("每回合开始时获得2点力量。")
                .flavor("胸腔里那点火不肯灭，只好让它烧。")
                .effects(Effect.power(Status.DEMON_FORM, 2))
                .plus("魔心+", "每回合开始时获得3点力量。", 3,
                        Effect.power(Status.DEMON_FORM, 3))
                .build());

        list.add(skill("ash_blood_pact", "血契", 1, Rarity.RARE)
                .description("失去6点生命，抽3张牌，获得2点能量。消耗。")
                .flavor("签的时候没读条款，签完才发现是自己。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.loseHp(6), Effect.draw(3), Effect.energy(2))
                .plus("血契+", "失去6点生命，抽4张牌，获得2点能量。消耗。", 1,
                        Effect.loseHp(6), Effect.draw(4), Effect.energy(2))
                .build());

        list.add(power("ash_immortal_ash", "不灭灰", 2, Rarity.RARE)
                .description("每当你失去生命，获得1点力量。")
                .flavor("烧过的东西不会真的消失，它只是变得更硬。")
                .effects(Effect.power(Status.RUPTURE, 1))
                .plus("不灭灰+", "每当你失去生命，获得2点力量。", 2,
                        Effect.power(Status.RUPTURE, 2))
                .build());

        list.add(skill("ash_venom_heart", "毒心", 2, Rarity.RARE)
                .description("使所有敌人获得6层中毒，抽1张牌。")
                .flavor("心跳一次，血里的墨就浓一分。")
                .effects(Effect.applyAll(Status.POISON, 6), Effect.draw(1))
                .plus("毒心+", "使所有敌人获得9层中毒，抽1张牌。", 2,
                        Effect.applyAll(Status.POISON, 9), Effect.draw(1))
                .build());

        list.add(skill("ash_last_breath", "最后一口气", 2, Rarity.RARE)
                .description("回复10点生命，获得10点格挡。消耗。")
                .flavor("吸进去的灰，最后都变成了墙。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.heal(10), Effect.block(10))
                .plus("最后一口气+", "回复14点生命，获得14点格挡。消耗。", 2,
                        Effect.heal(14), Effect.block(14))
                .build());

        list.add(power("ash_burning_ritual", "燃祭", 2, Rarity.RARE)
                .description("每回合开始时获得1点能量。")
                .flavor("每天喂它一页，它就替你多活一天。")
                .effects(Effect.power(Status.BERSERK, 1))
                .plus("燃祭+", "每回合开始时获得1点能量；每回合开始时失去1点生命，抽1张牌。", 2,
                        Effect.power(Status.BERSERK, 1), Effect.power(Status.BRUTALITY, 1))
                .build());

        list.add(attackAll("ash_cinder_storm", "烬暴", 2, Rarity.RARE)
                .description("对所有敌人造成3次5点伤害。")
                .flavor("风把整座塔的灰都卷了起来，像一场倒着下的雪。")
                .effects(Effect.damageAll(5, 3))
                .plus("烬暴+", "对所有敌人造成3次6点伤害。", 2, Effect.damageAll(6, 3))
                .build());

        return list;
    }
}
