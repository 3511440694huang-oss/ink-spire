package com.inkspire.rogue.content;

import com.inkspire.rogue.model.CardDef;
import com.inkspire.rogue.model.CardType;
import com.inkspire.rogue.model.Effect;
import com.inkspire.rogue.model.Rarity;
import com.inkspire.rogue.model.Status;
import com.inkspire.rogue.model.Target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 角色「游丝」的卡牌池（30 张）。
 *
 * 身份：敏捷叠盾、虚弱压制、抽牌节奏、多段细伤。不使用力量倍率（那是铁刻的）。
 * 升阶形态统一走 plus*，描述里的数字必须与 effects 完全一致。
 */
public final class CardsSilk {

    private CardsSilk() {
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

    public static List<CardDef> build() {
        List<CardDef> list = new ArrayList<CardDef>();

        /* ---------------- 初始牌 ---------------- */

        list.add(attack("silk_strike", "割丝", 1, Rarity.BASIC)
                .description("造成6点伤害。")
                .flavor("线一断，整页都松了。")
                .effects(Effect.damage(6))
                .plus("割丝+", "造成9点伤害。", 1, Effect.damage(9))
                .build());

        list.add(skill("silk_defend", "收线", 1, Rarity.BASIC)
                .description("获得5点格挡。")
                .flavor("把散开的线头一圈圈缠回腕上。")
                .effects(Effect.block(5))
                .plus("收线+", "获得8点格挡。", 1, Effect.block(8))
                .build());

        list.add(attack("silk_signature", "缠绕", 1, Rarity.BASIC)
                .description("造成5点伤害，使目标获得1层虚弱。")
                .flavor("先绊住脚踝，再谈别的。")
                .effects(Effect.damage(5), Effect.apply(Status.WEAK, 1))
                .plus("缠绕+", "造成7点伤害，使目标获得2层虚弱。", 1,
                        Effect.damage(7), Effect.apply(Status.WEAK, 2))
                .build());

        /* ---------------- 普通 · 攻击 ---------------- */

        list.add(attack("silk_pair", "双股", 1, Rarity.COMMON)
                .description("造成2次4点伤害。")
                .flavor("两股线拧在一起，才经得起拉。")
                .effects(Effect.damage(4, 2))
                .plus("双股+", "造成2次6点伤害。", 1, Effect.damage(6, 2))
                .build());

        list.add(attack("silk_snare", "绊线", 1, Rarity.COMMON)
                .description("造成6点伤害，使目标获得1层虚弱。")
                .flavor("绷直的一根，藏在两行字之间。")
                .effects(Effect.damage(6), Effect.apply(Status.WEAK, 1))
                .plus("绊线+", "造成8点伤害，使目标获得2层虚弱。", 1,
                        Effect.damage(8), Effect.apply(Status.WEAK, 2))
                .build());

        list.add(attack("silk_check", "校线", 1, Rarity.COMMON)
                .description("造成5点伤害，抽1张牌。")
                .flavor("指腹沿行距划过去，摸到一处打结。")
                .effects(Effect.damage(5), Effect.draw(1))
                .plus("校线+", "造成7点伤害，抽1张牌。", 1,
                        Effect.damage(7), Effect.draw(1))
                .build());

        list.add(attack("silk_flick", "弹指", 0, Rarity.COMMON)
                .description("造成4点伤害。消耗。")
                .flavor("线头脱手的一瞬，正好抽在他脸上。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.damage(4))
                .plus("弹指+", "造成6点伤害。消耗。", 0, Effect.damage(6))
                .build());

        list.add(attack("silk_needle", "引针", 1, Rarity.COMMON)
                .description("造成3次3点伤害。")
                .flavor("一针不够，就三针。")
                .effects(Effect.damage(3, 3))
                .plus("引针+", "造成3次4点伤害。", 1, Effect.damage(4, 3))
                .build());

        list.add(attackAll("silk_flurry", "纷飞", 1, Rarity.COMMON)
                .description("对所有敌人造成6点伤害。")
                .flavor("风一起，满架的线都散了。")
                .effects(Effect.damageAll(6))
                .plus("纷飞+", "对所有敌人造成8点伤害。", 1, Effect.damageAll(8))
                .build());

        list.add(attack("silk_taut", "绷紧", 2, Rarity.COMMON)
                .description("造成16点伤害。")
                .flavor("拉到极限的那一根，比刀还快。")
                .effects(Effect.damage(16))
                .plus("绷紧+", "造成20点伤害。", 2, Effect.damage(20))
                .build());

        /* ---------------- 普通 · 技能 ---------------- */

        list.add(skill("silk_step", "错步", 1, Rarity.COMMON)
                .description("获得7点格挡。")
                .flavor("侧身半步，句子就落空了。")
                .effects(Effect.block(7))
                .plus("错步+", "获得10点格挡。", 1, Effect.block(10))
                .build());

        list.add(skill("silk_veil", "薄纱", 2, Rarity.COMMON)
                .description("获得16点格挡。")
                .flavor("一层纱，隔开一整章。")
                .effects(Effect.block(16))
                .plus("薄纱+", "获得20点格挡。", 2, Effect.block(20))
                .build());

        list.add(skill("silk_leads", "牵手", 1, Rarity.COMMON)
                .description("获得3点格挡，抽2张牌。")
                .flavor("牵着走，比推着走省力。")
                .effects(Effect.block(3), Effect.draw(2))
                .plus("牵手+", "获得5点格挡，抽2张牌。", 1,
                        Effect.block(5), Effect.draw(2))
                .build());

        list.add(skillOnEnemy("silk_twist", "绞腕", 1, Rarity.COMMON)
                .description("造成3点伤害，使目标失去1点力量。")
                .flavor("腕子一绞，握笔的手就软了。")
                .effects(Effect.damage(3), Effect.apply(Status.STRENGTH, -1))
                .plus("绞腕+", "造成5点伤害，使目标失去2点力量。", 1,
                        Effect.damage(5), Effect.apply(Status.STRENGTH, -2))
                .build());

        /* ---------------- 罕见 ---------------- */

        list.add(skillOnAll("silk_weave", "织网", 2, Rarity.UNCOMMON)
                .description("获得8点格挡，使所有敌人获得3层虚弱。")
                .flavor("网织到最后一格，才看出它有多大。")
                .effects(Effect.block(8), Effect.applyAll(Status.WEAK, 3))
                .plus("织网+", "获得11点格挡，使所有敌人获得4层虚弱。", 2,
                        Effect.block(11), Effect.applyAll(Status.WEAK, 4))
                .build());

        list.add(skill("silk_blur", "幻影", 1, Rarity.UNCOMMON)
                .description("获得11点格挡。虚无。")
                .flavor("你以为看见了人，其实只是一页抖动的纸。")
                .ethereal()
                .effects(Effect.block(11))
                .plus("幻影+", "获得15点格挡。虚无。", 1, Effect.block(15))
                .build());

        list.add(attack("silk_stitch", "缝合", 1, Rarity.UNCOMMON)
                .description("造成2次5点伤害。")
                .flavor("穿过两层纸背，再缝回来。")
                .effects(Effect.damage(5, 2))
                .plus("缝合+", "造成2次6点伤害。", 1, Effect.damage(6, 2))
                .build());

        list.add(attackAll("silk_scissor", "剪线", 2, Rarity.UNCOMMON)
                .description("对所有敌人造成9点伤害，使所有敌人获得1层易伤。")
                .flavor("咔嚓一声，所有线头同时落地。")
                .effects(Effect.damageAll(9), Effect.applyAll(Status.VULNERABLE, 1))
                .plus("剪线+", "对所有敌人造成12点伤害，使所有敌人获得2层易伤。", 2,
                        Effect.damageAll(12), Effect.applyAll(Status.VULNERABLE, 2))
                .build());

        list.add(power("silk_focus", "凝线", 1, Rarity.UNCOMMON)
                .description("获得2点敏捷。")
                .flavor("把线捻细，一指宽的地方能穿七根。")
                .effects(Effect.power(Status.DEXTERITY, 2))
                .plus("凝线+", "获得3点敏捷。", 1, Effect.power(Status.DEXTERITY, 3))
                .build());

        list.add(skill("silk_mend", "补页", 1, Rarity.UNCOMMON)
                .description("回复6点生命，抽1张牌。")
                .flavor("裂口用同色的线走一遍，就看不见了。")
                .effects(Effect.heal(6), Effect.draw(1))
                .plus("补页+", "回复9点生命，抽1张牌。", 1, Effect.heal(9), Effect.draw(1))
                .build());

        list.add(skillOnEnemy("silk_snag", "拦丝", 0, Rarity.UNCOMMON)
                .description("使目标获得2层虚弱。消耗。")
                .flavor("随手一勾，脚下就多了一道。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.apply(Status.WEAK, 2))
                .plus("拦丝+", "使目标获得3层虚弱。消耗。", 0, Effect.apply(Status.WEAK, 3))
                .build());

        list.add(power("silk_tension", "张力", 1, Rarity.UNCOMMON)
                .description("每当你消耗一张牌，获得2点格挡。")
                .flavor("断掉的线不会消失，它只是换了个地方绷着。")
                .effects(Effect.power(Status.FEEL_NO_PAIN, 2))
                .plus("张力+", "每当你消耗一张牌，获得3点格挡。", 1,
                        Effect.power(Status.FEEL_NO_PAIN, 3))
                .build());

        list.add(skill("silk_loop", "回环", 1, Rarity.UNCOMMON)
                .description("抽3张牌。消耗。")
                .flavor("线绕回来，落在刚才落笔的地方。")
                .exhaust()
                .plusExhaust()
                .effects(Effect.draw(3))
                .plus("回环+", "抽4张牌。消耗。", 1, Effect.draw(4))
                .build());

        list.add(attack("silk_knot", "死结", 2, Rarity.UNCOMMON)
                .description("造成10点伤害，使目标失去2点力量，直到它下个回合结束。")
                .flavor("越是解不开，越是想解。")
                .effects(Effect.damage(10), Effect.apply(Status.SHACKLE, 2))
                .plus("死结+", "造成13点伤害，使目标失去3点力量，直到它下个回合结束。", 2,
                        Effect.damage(13), Effect.apply(Status.SHACKLE, 3))
                .build());


        /* ---------------- 罕见（补充：节奏、虚弱、细伤） ---------------- */

        list.add(skill("silk_braid", "编绳", 1, Rarity.UNCOMMON)
                .description("获得7点格挡，抽1张牌。")
                .flavor("三股并成一股，握在手里才不打滑。")
                .effects(Effect.block(7), Effect.draw(1))
                .plus("编绳+", "获得10点格挡，抽1张牌。", 1, Effect.block(10), Effect.draw(1))
                .build());

        list.add(attack("silk_pin", "别针", 0, Rarity.UNCOMMON)
                .description("造成3点伤害，使目标获得1层虚弱。")
                .flavor("针尖上那一点墨，够它麻一下午。")
                .effects(Effect.damage(3), Effect.apply(Status.WEAK, 1))
                .plus("别针+", "造成4点伤害，使目标获得2层虚弱。", 0,
                        Effect.damage(4), Effect.apply(Status.WEAK, 2))
                .build());

        list.add(attackAll("silk_whirl", "旋丝", 1, Rarity.UNCOMMON)
                .description("对所有敌人造成5点伤害，使所有敌人获得1层虚弱。")
                .flavor("一甩手，丝就贴着地面刮了一圈。")
                .effects(Effect.damageAll(5), Effect.applyAll(Status.WEAK, 1))
                .plus("旋丝+", "对所有敌人造成7点伤害，使所有敌人获得2层虚弱。", 1,
                        Effect.damageAll(7), Effect.applyAll(Status.WEAK, 2))
                .build());

        list.add(skill("silk_glance", "侧目", 1, Rarity.UNCOMMON)
                .description("抽2张牌，获得4点格挡。")
                .flavor("不必回头，眼角的线已经替你数清了。")
                .effects(Effect.draw(2), Effect.block(4))
                .plus("侧目+", "抽2张牌，获得7点格挡。", 1, Effect.draw(2), Effect.block(7))
                .build());

        list.add(skillOnEnemy("silk_pull", "引线", 1, Rarity.UNCOMMON)
                .description("使目标获得2层易伤，抽1张牌。")
                .flavor("把线头递到它手里，它自己会拽。")
                .effects(Effect.apply(Status.VULNERABLE, 2), Effect.draw(1))
                .plus("引线+", "使目标获得3层易伤，抽2张牌。", 1,
                        Effect.apply(Status.VULNERABLE, 3), Effect.draw(2))
                .build());

        list.add(skill("silk_curtain", "帷幕", 2, Rarity.UNCOMMON)
                .description("获得13点格挡，使所有敌人获得1层虚弱。")
                .flavor("整面墙都是线织的，连风都被挡在外面。")
                .effects(Effect.block(13), Effect.applyAll(Status.WEAK, 1))
                .plus("帷幕+", "获得16点格挡，使所有敌人获得2层虚弱。", 2,
                        Effect.block(16), Effect.applyAll(Status.WEAK, 2))
                .build());

        list.add(attack("silk_twine", "麻绳", 1, Rarity.UNCOMMON)
                .description("造成8点伤害，使目标获得1层易伤。")
                .flavor("粗的才缠得紧，细的只配穿针。")
                .effects(Effect.damage(8), Effect.apply(Status.VULNERABLE, 1))
                .plus("麻绳+", "造成10点伤害，使目标获得2层易伤。", 1,
                        Effect.damage(10), Effect.apply(Status.VULNERABLE, 2))
                .build());

        list.add(skill("silk_hem", "收边", 1, Rarity.UNCOMMON)
                .description("回复4点生命，获得4点格挡。")
                .flavor("布的边缘要卷两道，才不会一路抽丝。")
                .effects(Effect.heal(4), Effect.block(4))
                .plus("收边+", "回复6点生命，获得6点格挡。", 1, Effect.heal(6), Effect.block(6))
                .build());

        list.add(attack("silk_cruel", "细割", 1, Rarity.UNCOMMON)
                .description("造成3次4点伤害。")
                .flavor("不剪断，只是一寸一寸地磨。")
                .effects(Effect.damage(4, 3))
                .plus("细割+", "造成3次5点伤害。", 1, Effect.damage(5, 3))
                .build());

        list.add(skill("silk_lattice", "经纬", 2, Rarity.UNCOMMON)
                .description("获得10点格挡，获得1点敏捷。")
                .flavor("经不动，纬不散，格子才立得住。")
                .effects(Effect.block(10), Effect.power(Status.DEXTERITY, 1))
                .plus("经纬+", "获得13点格挡，获得2点敏捷。", 2,
                        Effect.block(13), Effect.power(Status.DEXTERITY, 2))
                .build());

        list.add(skillOnEnemy("silk_sift", "筛线", 1, Rarity.UNCOMMON)
                .description("使目标获得1层虚弱与1层易伤，抽1张牌。")
                .flavor("抖一抖，松的断的全都落下去。")
                .effects(Effect.apply(Status.WEAK, 1), Effect.apply(Status.VULNERABLE, 1),
                        Effect.draw(1))
                .plus("筛线+", "使目标获得2层虚弱与2层易伤，抽1张牌。", 1,
                        Effect.apply(Status.WEAK, 2), Effect.apply(Status.VULNERABLE, 2),
                        Effect.draw(1))
                .build());

        /* ---------------- 稀有 ---------------- */

        list.add(power("silk_wall", "不落之墙", 2, Rarity.RARE)
                .description("格挡不再于回合开始时消失。")
                .flavor("线织到发硬，风撞上去都有了响声。")
                .effects(Effect.power(Status.BARRICADE, 1))
                .plus("不落之墙+", "格挡不再于回合开始时消失。", 1,
                        Effect.power(Status.BARRICADE, 1))
                .build());

        list.add(skill("silk_seamless", "天衣", 2, Rarity.RARE)
                .description("使你的格挡翻倍，然后获得5点格挡。")
                .flavor("没有一处接缝，谁也别想从里面抽走一行。")
                .effects(Effect.doubleBlock(), Effect.block(5))
                .plus("天衣+", "使你的格挡翻倍，然后获得8点格挡。", 2,
                        Effect.doubleBlock(), Effect.block(8))
                .build());

        list.add(attackAll("silk_razor", "丝刃", 3, Rarity.RARE)
                .description("对所有敌人造成14点伤害，抽1张牌。")
                .flavor("线拉得够细，风就会替你切。")
                .effects(Effect.damageAll(14), Effect.draw(1))
                .plus("丝刃+", "对所有敌人造成18点伤害，抽2张牌。", 3,
                        Effect.damageAll(18), Effect.draw(2))
                .build());

        list.add(power("silk_breathe", "织息", 2, Rarity.RARE)
                .description("每回合结束时获得5点格挡。")
                .flavor("呼吸的节奏，和梭子一样。")
                .effects(Effect.power(Status.METALLICIZE, 5))
                .plus("织息+", "每回合结束时获得7点格挡。", 2,
                        Effect.power(Status.METALLICIZE, 7))
                .build());

        list.add(attack("silk_finale", "终线", 3, Rarity.RARE)
                .description("造成3次9点伤害。")
                .flavor("最后一根线收口，整本书都被缝住了。")
                .effects(Effect.damage(9, 3))
                .plus("终线+", "造成3次11点伤害。", 3, Effect.damage(11, 3))
                .build());


        list.add(power("silk_mirror", "镜丝", 2, Rarity.RARE)
                .description("每当你获得格挡，对随机敌人造成2点伤害。")
                .flavor("线绷在两面镜子之间，谁碰一下，另一半也响。")
                .effects(Effect.power(Status.JUGGERNAUT, 2))
                .plus("镜丝+", "每当你获得格挡，对随机敌人造成3点伤害。", 2,
                        Effect.power(Status.JUGGERNAUT, 3))
                .build());

        list.add(skillOnEnemy("silk_puppet", "牵丝", 1, Rarity.RARE)
                .description("使目标获得3层易伤与3层虚弱，抽1张牌。")
                .flavor("它以为自己在走，其实是你在提。")
                .effects(Effect.apply(Status.VULNERABLE, 3), Effect.apply(Status.WEAK, 3),
                        Effect.draw(1))
                .plus("牵丝+", "使目标获得4层易伤与4层虚弱，抽1张牌。", 1,
                        Effect.apply(Status.VULNERABLE, 4), Effect.apply(Status.WEAK, 4),
                        Effect.draw(1))
                .build());

        list.add(attack("silk_needle_storm", "针雨", 2, Rarity.RARE)
                .description("造成6次3点随机伤害。")
                .flavor("整盒针都倒出来了，落在谁身上只有天知道。")
                .effects(Effect.damageRandom(3, 6))
                .plus("针雨+", "造成6次4点随机伤害。", 2, Effect.damageRandom(4, 6))
                .build());

        list.add(skill("silk_lifeline", "命线", 2, Rarity.RARE)
                .description("回复8点生命，获得8点格挡，抽1张牌。")
                .flavor("最后一根线，一头在本子上，一头还系着人。")
                .effects(Effect.heal(8), Effect.block(8), Effect.draw(1))
                .plus("命线+", "回复12点生命，获得12点格挡，抽1张牌。", 2,
                        Effect.heal(12), Effect.block(12), Effect.draw(1))
                .build());

        list.add(attackAll("silk_ten_thousand", "万缕", 3, Rarity.RARE)
                .description("对所有敌人造成3次6点伤害。")
                .flavor("一万根线同时绷紧，声音像一场雨。")
                .effects(Effect.damageAll(6, 3))
                .plus("万缕+", "对所有敌人造成3次7点伤害。", 3, Effect.damageAll(7, 3))
                .build());

        list.add(skill("silk_anchor", "沉丝", 2, Rarity.RARE)
                .description("获得20点格挡。")
                .flavor("丝浸了墨，比铁还沉。")
                .effects(Effect.block(20))
                .plus("沉丝+", "获得26点格挡。", 2, Effect.block(26))
                .build());

        list.add(skillOnAll("silk_whisper", "絮语", 2, Rarity.RARE)
                .description("使所有敌人获得2层虚弱与2层易伤，抽1张牌。")
                .flavor("它们听见的只是自己的名字，一遍又一遍。")
                .effects(Effect.applyAll(Status.WEAK, 2), Effect.applyAll(Status.VULNERABLE, 2),
                        Effect.draw(1))
                .plus("絮语+", "使所有敌人获得3层虚弱与3层易伤，抽2张牌。", 2,
                        Effect.applyAll(Status.WEAK, 3), Effect.applyAll(Status.VULNERABLE, 3),
                        Effect.draw(2))
                .build());

        list.add(attack("silk_cut", "断丝", 0, Rarity.RARE)
                .description("造成4点伤害，抽1张牌。")
                .flavor("本来就该断的，剪子只是顺手。")
                .effects(Effect.damage(4), Effect.draw(1))
                .plus("断丝+", "造成6点伤害，抽1张牌。", 0, Effect.damage(6), Effect.draw(1))
                .build());

        return Collections.unmodifiableList(list);
    }
}
