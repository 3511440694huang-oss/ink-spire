package com.inkspire.rogue.content;

import com.inkspire.rogue.core.EnemyState;
import com.inkspire.rogue.model.Effect;
import com.inkspire.rogue.model.EnemyDef;
import com.inkspire.rogue.model.MoveDef;
import com.inkspire.rogue.model.Status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Bestiary of the ink tower. Four acts, each with eight normal enemies, three elites and one boss.
 * Every move uses only the effect kinds the combat engine can resolve for enemies.
 */
public final class EnemyLibrary {
    private static final Map<String, EnemyDef> ENEMIES = new LinkedHashMap<>();
    private static final List<List<EnemyDef>> NORMAL = new ArrayList<>();
    private static final List<List<EnemyDef>> ELITE = new ArrayList<>();
    private static final EnemyDef[] BOSS = new EnemyDef[4];

    static {
        for (int i = 0; i < 4; i++) {
            NORMAL.add(new ArrayList<EnemyDef>());
            ELITE.add(new ArrayList<EnemyDef>());
        }

        // ===================== Act I · 腐化回廊 =====================
        add(new EnemyDef("ink_worm", "墨蛆", "蛆",
                "走廊墙缝里蠕动的一截旧墨。它不识字，只吃字与字之间的空白。",
                EnemyDef.Tier.NORMAL, 0, 28, 33,
                Arrays.asList(
                        MoveDef.of("gnaw", "啃页", 10, Effect.damage(6)).maxRepeat(2),
                        MoveDef.of("curl", "蜷缩", 8, Effect.block(6)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("seep", "渗墨", 7, Effect.damage(3), Effect.apply(Status.WEAK, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("squirm", "蠕动", 5, Effect.block(4), Effect.power(Status.STRENGTH, 1)).cooldown(2).maxRepeat(1).intent("蓄力")
                )));

        add(new EnemyDef("page_wraith", "残页游魂", "页",
                "一张被撕下的书页学会了自己走路。它记得的句子越多，影子就越薄。",
                EnemyDef.Tier.NORMAL, 0, 30, 36,
                Arrays.asList(
                        MoveDef.of("slash", "割页", 10, Effect.damage(8)),
                        MoveDef.of("rewrite", "复写", 6, Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("强化"),
                        MoveDef.of("whisper", "低语", 7, Effect.apply(Status.VULNERABLE, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("flutter", "飘散", 5, Effect.block(7), Effect.apply(Status.WEAK, 1)).cooldown(1).maxRepeat(1)
                )));

        add(new EnemyDef("rust_clerk", "锈钉书吏", "钉",
                "生前负责装订，死后仍把每一页钉回原位。钉子生锈，句子也跟着发黑。",
                EnemyDef.Tier.NORMAL, 0, 34, 40,
                Arrays.asList(
                        MoveDef.of("pierce", "钉穿", 10, Effect.damage(9)).maxRepeat(1),
                        MoveDef.of("bind", "装订", 7, Effect.block(8), Effect.power(Status.STRENGTH, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("file", "归档", 6, Effect.apply(Status.WEAK, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("hammer", "锤平", 6, Effect.damage(5), Effect.block(5)).cooldown(1).maxRepeat(2)
                )));

        add(new EnemyDef("wax_acolyte", "蜡封信徒", "蜡",
                "他们把祷词写在蜡上，再把自己封进信里。火一靠近，信仰就开始滴落。",
                EnemyDef.Tier.NORMAL, 0, 32, 38,
                Arrays.asList(
                        MoveDef.of("prayer", "祷词", 8, Effect.power(Status.STRENGTH, 3)).minTurn(1).cooldown(2).maxRepeat(1).intent("强化"),
                        MoveDef.of("drip", "蜡滴", 8, Effect.damage(7), Effect.apply(Status.WEAK, 1)),
                        MoveDef.of("seal", "凝固", 6, Effect.block(9)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("zeal", "自焚", 5, Effect.loseHp(4), Effect.damageAll(10)).cooldown(3).maxRepeat(1).intent("狂热")
                )));

        add(new EnemyDef("bookworm_swarm", "蛀卷虫群", "蛀",
                "一小片会移动的沙沙声。翻到哪一页，哪一页就少一行。",
                EnemyDef.Tier.NORMAL, 0, 28, 34,
                Arrays.asList(
                        MoveDef.of("swarm", "群噬", 10, Effect.damage(3, 3)).maxRepeat(2),
                        MoveDef.of("burrow", "钻页", 7, Effect.damage(5), Effect.apply(Status.VULNERABLE, 1)),
                        MoveDef.of("nibble", "蚕食", 5, Effect.block(5), Effect.heal(3)).cooldown(2).maxRepeat(1),
                        MoveDef.of("molt", "蜕壳", 5, Effect.block(4), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("蓄力")
                )));

        add(new EnemyDef("ink_moth", "墨蛾", "蛾",
                "它扑向每一盏读书的灯，翅膀上落满别人还没写完的标点。",
                EnemyDef.Tier.NORMAL, 0, 30, 38,
                Arrays.asList(
                        MoveDef.of("scale_dust", "鳞粉", 9, Effect.damage(7), Effect.apply(Status.WEAK, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("flit", "扑灯", 9, Effect.damage(4, 2)).maxRepeat(1),
                        MoveDef.of("smudge", "蹭灰", 7, Effect.block(7), Effect.apply(Status.FRAIL, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("shed", "褪翅", 5, Effect.block(4), Effect.power(Status.DEXTERITY, 2)).cooldown(2).maxRepeat(1).intent("防御")
                )));

        add(new EnemyDef("stain_blob", "渍斑", "渍",
                "一滩没写完的墨，在墙脚慢慢摊开。它不追人，只是等你踩进来。",
                EnemyDef.Tier.NORMAL, 0, 26, 30,
                Arrays.asList(
                        MoveDef.of("smear", "涂抹", 9, Effect.damage(5), Effect.apply(Status.WEAK, 1)),
                        MoveDef.of("harden", "结壳", 6, Effect.block(8)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("cover", "覆盖", 7, Effect.damage(3, 2)).maxRepeat(2)
                )));

        add(new EnemyDef("paper_slice", "纸刃", "刃",
                "一张裁得极薄的纸，边缘比刀还直。它飞起来的时候几乎没有声音。",
                EnemyDef.Tier.NORMAL, 0, 22, 26,
                Arrays.asList(
                        MoveDef.of("cut", "割", 10, Effect.damage(4, 2)),
                        MoveDef.of("curl", "卷边", 5, Effect.block(5), Effect.power(Status.STRENGTH, 1))
                                .cooldown(2).maxRepeat(1).intent("蓄力"),
                        MoveDef.of("throat", "划喉", 6, Effect.damage(9)).cooldown(1).maxRepeat(1)
                )));

        add(new EnemyDef("binding_master", "装订巨匠", "装",
                "他把活人钉进书脊，说这样故事才不会散架。针脚整齐得令人不安。",
                EnemyDef.Tier.ELITE, 0, 82, 92,
                Arrays.asList(
                        MoveDef.of("spine_press", "压脊", 10, Effect.damage(14)).maxRepeat(1),
                        MoveDef.of("lockstitch", "锁线", 7, Effect.block(10), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1),
                        MoveDef.of("trim", "裁切", 7, Effect.damage(8), Effect.apply(Status.VULNERABLE, 2)).cooldown(2).maxRepeat(1),
                        MoveDef.of("rebind", "重装", 5, Effect.block(8), Effect.heal(8)).cooldown(3).maxRepeat(1)
                )));

        add(new EnemyDef("corrosive_proofreader", "蚀学校勘", "勘",
                "红笔划过的地方，纸会腐烂。它挑剔每一个错字，包括你。",
                EnemyDef.Tier.ELITE, 0, 78, 90,
                Arrays.asList(
                        MoveDef.of("red_mark", "朱批", 9, Effect.damage(10), Effect.apply(Status.WEAK, 2)).maxRepeat(1),
                        MoveDef.of("acid", "酸蚀", 8, Effect.damage(6), Effect.apply(Status.VULNERABLE, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("mend", "修补", 5, Effect.block(12), Effect.heal(6)).cooldown(2).maxRepeat(1),
                        MoveDef.of("revise", "改稿", 6, Effect.block(6), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("paste_mounter", "裱糊匠", "裱",
                "他把散架的尸首一页页裱平，说这样至少还读得下去。浆糊是陈的。",
                EnemyDef.Tier.ELITE, 0, 80, 92,
                Arrays.asList(
                        MoveDef.of("brush_over", "上浆", 10, Effect.damage(12)).maxRepeat(1),
                        MoveDef.of("stale_paste", "陈浆", 8, Effect.damage(6), Effect.apply(Status.POISON, 3)).cooldown(1).maxRepeat(1),
                        MoveDef.of("paste_wall", "糊墙", 6, Effect.block(12), Effect.power(Status.STRENGTH, 1)).cooldown(2).maxRepeat(1).intent("防御"),
                        MoveDef.of("trim_edge", "裁边", 7, Effect.damage(6), Effect.apply(Status.VULNERABLE, 2)).cooldown(1).maxRepeat(1)
                )));

        add(new EnemyDef("grub_sovereign", "蠹王", "蠹",
                "腐化回廊的尽头，一册被蛀空的书蜷成王座。它不吃纸，吃故事剩下的部分。",
                EnemyDef.Tier.BOSS, 0, 115, 130,
                Arrays.asList(
                        MoveDef.of("pagefall", "千页压顶", 10, Effect.damage(16)).maxRepeat(1),
                        MoveDef.of("ink_rain", "墨雨", 8, Effect.damageAll(5), Effect.damageAll(5), Effect.damageAll(5)).cooldown(2).maxRepeat(1),
                        MoveDef.of("devour", "蛀蚀", 7, Effect.apply(Status.WEAK, 2), Effect.apply(Status.VULNERABLE, 2)).cooldown(2).maxRepeat(1),
                        MoveDef.of("crown", "加冕", 6, Effect.block(10), Effect.power(Status.STRENGTH, 3)).cooldown(3).maxRepeat(1).intent("强化"),
                        MoveDef.of("final_volume", "万卷归一", 8, Effect.damage(9, 3), Effect.power(Status.STRENGTH, 1)).cooldown(2).maxRepeat(1).phase2()
                )));

        // ===================== Act II · 无声书库 =====================
        add(new EnemyDef("silent_monk", "缄默僧", "默",
                "他发过誓，读完之前绝不出声。于是书库把所有声音都借给了他。",
                EnemyDef.Tier.NORMAL, 1, 40, 46,
                Arrays.asList(
                        MoveDef.of("mute_palm", "无言掌", 10, Effect.damage(9)),
                        MoveDef.of("vow", "闭口禅", 7, Effect.block(9), Effect.apply(Status.WEAK, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("hush", "寂静", 7, Effect.apply(Status.VULNERABLE, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("inner_voice", "心声", 6, Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("蓄力")
                )));

        add(new EnemyDef("index_spider", "索引蛛", "索",
                "它不结网，只结目录。被它标中的句子，迟早会被拖进注脚。",
                EnemyDef.Tier.NORMAL, 1, 38, 44,
                Arrays.asList(
                        MoveDef.of("cross_ref", "交叉索引", 9, Effect.damage(6, 2)),
                        MoveDef.of("silk", "缠丝", 7, Effect.apply(Status.WEAK, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("cite", "引注", 7, Effect.damage(7), Effect.apply(Status.VULNERABLE, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("catalog", "编目", 5, Effect.block(8), Effect.power(Status.STRENGTH, 1)).cooldown(2).maxRepeat(1)
                )));

        add(new EnemyDef("blank_scribe", "空白抄工", "抄",
                "他抄了一辈子。最后一次抬头时，发现自己的脸也被抄走了。",
                EnemyDef.Tier.NORMAL, 1, 42, 48,
                Arrays.asList(
                        MoveDef.of("transcribe", "誊抄", 10, Effect.damage(8)),
                        MoveDef.of("blot", "涂改", 8, Effect.damage(6), Effect.apply(Status.VULNERABLE, 1)),
                        MoveDef.of("erase", "擦除", 6, Effect.block(5), Effect.heal(6)).cooldown(2).maxRepeat(1),
                        MoveDef.of("copy", "临摹", 5, Effect.block(4), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("ink_pool", "墨池", "池",
                "一滩没有倒影的墨。你俯身时，它先看见了你。",
                EnemyDef.Tier.NORMAL, 1, 44, 50,
                Arrays.asList(
                        MoveDef.of("engulf", "吞墨", 10, Effect.damage(11)).maxRepeat(1),
                        MoveDef.of("settle", "沉底", 7, Effect.block(12)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("ripple", "泛墨", 6, Effect.applyAll(Status.WEAK, 1)).cooldown(2).maxRepeat(1),
                        MoveDef.of("deepen", "加深", 5, Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("蓄力")
                )));

        add(new EnemyDef("silent_pendulum", "无音摆", "摆",
                "钟摆来回，却听不见滴答。被它扫过的人，会忘记自己刚才想说什么。",
                EnemyDef.Tier.NORMAL, 1, 40, 46,
                Arrays.asList(
                        MoveDef.of("swing", "摆锤", 10, Effect.damage(6, 2)),
                        MoveDef.of("stop", "停摆", 6, Effect.block(8), Effect.heal(4)).cooldown(2).maxRepeat(1),
                        MoveDef.of("mesmerize", "催眠", 6, Effect.apply(Status.WEAK, 2), Effect.apply(Status.VULNERABLE, 1)).cooldown(2).maxRepeat(1),
                        MoveDef.of("toll", "空鸣", 6, Effect.damageAll(9)).cooldown(2).maxRepeat(1)
                )));

        add(new EnemyDef("hush_lantern", "噤灯侍", "灯",
                "他提着一盏不亮的灯巡库。被那圈光照到的东西，声音会先熄灭。",
                EnemyDef.Tier.NORMAL, 1, 40, 50,
                Arrays.asList(
                        MoveDef.of("snuff", "熄声", 10, Effect.damage(10)).maxRepeat(1),
                        MoveDef.of("lamp_oil", "灯油", 8, Effect.damage(9), Effect.apply(Status.WEAK, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("shade", "遮光", 7, Effect.block(11)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("wick", "捻芯", 6, Effect.heal(4), Effect.power(Status.STRENGTH, 2)).cooldown(3).maxRepeat(1).intent("蓄力")
                )));

        add(new EnemyDef("glue_pot", "胶锅", "胶",
                "一口熬着旧胶的小锅，锅沿结着一圈发黄的边。它咕嘟一声，就有人被粘住。",
                EnemyDef.Tier.NORMAL, 1, 34, 38,
                Arrays.asList(
                        MoveDef.of("splash", "泼胶", 9, Effect.damage(6), Effect.apply(Status.FRAIL, 1)),
                        MoveDef.of("bubble", "冒泡", 6, Effect.damage(3), Effect.apply(Status.WEAK, 2))
                                .cooldown(1).maxRepeat(1),
                        MoveDef.of("thicken", "熬稠", 5, Effect.power(Status.STRENGTH, 2))
                                .cooldown(2).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("erasing_monk", "擦拭僧", "擦",
                "他手里没有经卷，只有一块用旧的橡皮。走过的地方，字都变成了灰。",
                EnemyDef.Tier.NORMAL, 1, 30, 34,
                Arrays.asList(
                        MoveDef.of("erase", "擦除", 9, Effect.damage(7), Effect.apply(Status.SHACKLE, 1)),
                        MoveDef.of("flatten", "抹平", 6, Effect.block(10)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("rewrite", "重写", 7, Effect.damage(5, 2))
                )));

        add(new EnemyDef("quiet_curator", "静默馆长", "馆",
                "他把所有喧哗装进玻璃柜，标签写着「待整理」。你也在柜子里。",
                EnemyDef.Tier.ELITE, 1, 92, 104,
                Arrays.asList(
                        MoveDef.of("silence", "禁言", 8, Effect.apply(Status.WEAK, 2), Effect.apply(Status.VULNERABLE, 2)).cooldown(2).maxRepeat(1),
                        MoveDef.of("gavel", "落槌", 9, Effect.damage(16), Effect.block(5)).cooldown(1).maxRepeat(1),
                        MoveDef.of("accession", "入藏", 6, Effect.block(8), Effect.power(Status.STRENGTH, 3)).cooldown(2).maxRepeat(1).intent("强化"),
                        MoveDef.of("deaccession", "注销", 7, Effect.damage(8, 2), Effect.apply(Status.VULNERABLE, 1)).cooldown(2).maxRepeat(1)
                )));

        add(new EnemyDef("index_mother", "索引之母", "母",
                "书库所有目录都从她腹中生出。她记得每一本书的位置，包括还没被写出来的。",
                EnemyDef.Tier.ELITE, 1, 96, 110,
                Arrays.asList(
                        MoveDef.of("reshelve", "归架", 9, Effect.damage(12), Effect.apply(Status.VULNERABLE, 2)).maxRepeat(1),
                        MoveDef.of("web", "蛛网", 7, Effect.block(14), Effect.apply(Status.WEAK, 1)).cooldown(2).maxRepeat(1),
                        MoveDef.of("retrieve", "检索", 7, Effect.damage(8, 2), Effect.power(Status.STRENGTH, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("whole_index", "总目", 5, Effect.block(10), Effect.heal(6), Effect.power(Status.STRENGTH, 1)).cooldown(3).maxRepeat(1)
                )));

        add(new EnemyDef("voice_registrar", "借声主簿", "簿",
                "书库里每一声低语都登记在他名下。到期不还的，连喉咙一起收回。",
                EnemyDef.Tier.ELITE, 1, 94, 108,
                Arrays.asList(
                        MoveDef.of("claim_voice", "收声", 10, Effect.damage(15)).maxRepeat(1),
                        MoveDef.of("ledger", "摊簿", 8, Effect.damage(11), Effect.apply(Status.VULNERABLE, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("fine", "罚金", 7, Effect.apply(Status.FRAIL, 2), Effect.apply(Status.WEAK, 1)).cooldown(2).maxRepeat(1),
                        MoveDef.of("shelve", "归档", 6, Effect.block(16), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("silent_volume", "缄默全书", "缄",
                "它没有一个字，却能让人听见自己所有的念头。书库的沉默，是它翻页的声音。",
                EnemyDef.Tier.BOSS, 1, 175, 195,
                Arrays.asList(
                        MoveDef.of("turning", "无风翻页", 10, Effect.damage(14, 2)).maxRepeat(1),
                        MoveDef.of("mute", "禁声", 7, Effect.apply(Status.WEAK, 3), Effect.apply(Status.VULNERABLE, 1)).cooldown(2).maxRepeat(1),
                        MoveDef.of("close", "合卷", 6, Effect.block(20), Effect.heal(10)).cooldown(3).maxRepeat(1),
                        MoveDef.of("awake", "书脊苏醒", 6, Effect.block(15), Effect.power(Status.STRENGTH, 4)).cooldown(3).maxRepeat(1).intent("觉醒"),
                        MoveDef.of("final_chapter", "终章朗读", 8, Effect.damage(8, 4), Effect.apply(Status.VULNERABLE, 2)).cooldown(2).maxRepeat(1).phase2()
                )));

        // ===================== Act III · 塔尖 =====================
        add(new EnemyDef("ash_penman", "灰烬执笔", "烬",
                "塔尖的风把纸烧成灰，他却仍用灰写字。每一笔都烫。",
                EnemyDef.Tier.NORMAL, 2, 50, 55,
                Arrays.asList(
                        MoveDef.of("burn_draft", "焚稿", 10, Effect.damage(12)).maxRepeat(1),
                        MoveDef.of("ember", "余烬", 7, Effect.damage(4), Effect.apply(Status.VULNERABLE, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("ash_guard", "灰盾", 6, Effect.block(12)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("rekindle", "重燃", 5, Effect.power(Status.STRENGTH, 3)).cooldown(2).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("spire_spirit", "塔灵", "塔",
                "塔每升高一层，它就薄一分。到了塔尖，它只剩下一声很尖的叹息。",
                EnemyDef.Tier.NORMAL, 2, 48, 54,
                Arrays.asList(
                        MoveDef.of("shriek", "尖啸", 9, Effect.damage(9), Effect.apply(Status.WEAK, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("phase", "穿墙", 9, Effect.damage(7, 2)),
                        MoveDef.of("stare", "凝视", 6, Effect.apply(Status.VULNERABLE, 2)).cooldown(2).maxRepeat(1),
                        MoveDef.of("ascend", "升腾", 5, Effect.block(8), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("蓄力")
                )));

        add(new EnemyDef("ink_guard", "墨甲卫", "甲",
                "墨干之后比铁还硬。它守的不是门，是最后一行没有写完的句子。",
                EnemyDef.Tier.NORMAL, 2, 52, 55,
                Arrays.asList(
                        MoveDef.of("suppress", "镇压", 10, Effect.damage(15)).maxRepeat(1),
                        MoveDef.of("armor", "墨甲", 7, Effect.block(15), Effect.power(Status.STRENGTH, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("counter", "反击", 6, Effect.damage(6), Effect.block(8)).cooldown(1).maxRepeat(1),
                        MoveDef.of("execute", "处决", 6, Effect.damage(10), Effect.apply(Status.VULNERABLE, 1)).cooldown(2).maxRepeat(1)
                )));

        add(new EnemyDef("chapter_hound", "断章猎犬", "犬",
                "它追着被撕掉的那一页跑了很久，久到自己也变成了一句没头没尾的话。",
                EnemyDef.Tier.NORMAL, 2, 48, 53,
                Arrays.asList(
                        MoveDef.of("maul", "撕咬", 10, Effect.damage(6, 2)),
                        MoveDef.of("hunt", "追猎", 6, Effect.block(4), Effect.power(Status.STRENGTH, 3)).cooldown(2).maxRepeat(1).intent("蓄力"),
                        MoveDef.of("howl", "低吼", 7, Effect.apply(Status.WEAK, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("pounce", "扑杀", 7, Effect.damage(13)).cooldown(1).maxRepeat(1)
                )));

        add(new EnemyDef("last_copyist", "终页抄录者", "终",
                "他抄写这座塔的最后一页，抄了一千年。每抄完一次，塔就多一层。",
                EnemyDef.Tier.NORMAL, 2, 50, 55,
                Arrays.asList(
                        MoveDef.of("copy", "誊抄", 6, Effect.block(6), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1),
                        MoveDef.of("strike_out", "勾销", 9, Effect.damage(13), Effect.apply(Status.VULNERABLE, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("seal_pen", "封笔", 5, Effect.block(8), Effect.heal(8)).cooldown(3).maxRepeat(1),
                        MoveDef.of("final_line", "末行", 8, Effect.damage(7, 2))
                )));

        add(new EnemyDef("wind_worn_leaf", "风蚀残页", "蚀",
                "塔尖的风把它磨得只剩一半。剩下那一半，还在坚持写下去。",
                EnemyDef.Tier.NORMAL, 2, 50, 57,
                Arrays.asList(
                        MoveDef.of("flay", "风剥", 10, Effect.damage(9, 2)).maxRepeat(1),
                        MoveDef.of("grit", "砂纸", 8, Effect.damage(12), Effect.apply(Status.FRAIL, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("fold", "折角", 7, Effect.block(14)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("dry", "风干", 6, Effect.block(6), Effect.power(Status.STRENGTH, 3)).cooldown(3).maxRepeat(1).intent("蓄力")
                )));

        add(new EnemyDef("margin_worm", "页边虫", "边",
                "它住在纸的最外面那一圈，靠啃食页边的空白长大。越靠近塔尖，它越粗。",
                EnemyDef.Tier.NORMAL, 2, 42, 48,
                Arrays.asList(
                        MoveDef.of("chew", "啃边", 8, Effect.damage(8, 2)),
                        MoveDef.of("coil", "蜷曲", 6, Effect.block(12)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("burrow", "钻入", 5, Effect.damage(14)).cooldown(2).maxRepeat(1)
                )));

        add(new EnemyDef("redaction_hand", "删改手", "删",
                "一只只剩三根指头的手，握着黑笔，专挑别人写好的句子下手。",
                EnemyDef.Tier.NORMAL, 2, 38, 44,
                Arrays.asList(
                        MoveDef.of("strike_out", "划掉", 9, Effect.damage(9), Effect.apply(Status.VULNERABLE, 2)),
                        MoveDef.of("blacken", "涂黑", 6, Effect.apply(Status.WEAK, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("amend", "补笔", 5, Effect.power(Status.STRENGTH, 2), Effect.block(8))
                                .cooldown(2).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("ouroboros_binder", "首尾装订者", "环",
                "它把书的开头缝进结尾，于是故事再也停不下来。它说，这叫永恒。",
                EnemyDef.Tier.ELITE, 2, 100, 110,
                Arrays.asList(
                        MoveDef.of("ouroboros", "首尾", 9, Effect.damage(18), Effect.apply(Status.VULNERABLE, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("loop", "循环", 6, Effect.block(18), Effect.heal(8)).cooldown(2).maxRepeat(1),
                        MoveDef.of("bite", "咬合", 7, Effect.damage(10, 2), Effect.power(Status.STRENGTH, 1)).cooldown(2).maxRepeat(1),
                        MoveDef.of("tighten", "收紧", 5, Effect.block(8), Effect.power(Status.STRENGTH, 2)).cooldown(3).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("spire_executioner", "塔尖行刑官", "刑",
                "他的笔是一把刀，判决书只有一句：故事到此为止。没有人见过第二页。",
                EnemyDef.Tier.ELITE, 2, 102, 110,
                Arrays.asList(
                        MoveDef.of("verdict", "宣判", 6, Effect.apply(Status.VULNERABLE, 3), Effect.apply(Status.WEAK, 2)).cooldown(3).maxRepeat(1),
                        MoveDef.of("fall", "落笔", 9, Effect.damage(20)).cooldown(1).maxRepeat(1),
                        MoveDef.of("storm", "墨刃风暴", 8, Effect.damage(7, 3)).cooldown(1).maxRepeat(1),
                        MoveDef.of("sharpen", "磨刃", 5, Effect.power(Status.STRENGTH, 3)).cooldown(2).maxRepeat(1).intent("蓄力")
                )));

        add(new EnemyDef("ash_typesetter", "铸字灰工", "铸",
                "他用熔化的铅字浇自己的骨头。浇一次，字就深一分，疼也是。",
                EnemyDef.Tier.ELITE, 2, 104, 114,
                Arrays.asList(
                        MoveDef.of("cast_line", "浇行", 10, Effect.damage(17)).maxRepeat(1),
                        MoveDef.of("lead_rain", "铅雨", 7, Effect.damage(7, 3)).cooldown(1).maxRepeat(1),
                        MoveDef.of("temper", "淬火", 6, Effect.block(18), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("强化"),
                        MoveDef.of("slag", "熔渣", 6, Effect.apply(Status.VULNERABLE, 2), Effect.apply(Status.WEAK, 2)).cooldown(2).maxRepeat(1)
                )));

        add(new EnemyDef("last_ink", "终焉之墨", "墨",
                "塔尖没有房间，只有一滴悬着的墨。它落下时，所有故事同时结束，又同时开始。",
                EnemyDef.Tier.BOSS, 2, 240, 260,
                Arrays.asList(
                        MoveDef.of("ink_tide", "墨潮", 10, Effect.damage(18)).maxRepeat(1),
                        MoveDef.of("ten_thousand_words", "万言穿心", 8, Effect.damage(7, 4)).cooldown(1).maxRepeat(1),
                        MoveDef.of("seal_page", "封页", 6, Effect.block(25), Effect.heal(12)).cooldown(3).maxRepeat(1),
                        MoveDef.of("swallow", "吞字", 7, Effect.apply(Status.WEAK, 3), Effect.apply(Status.VULNERABLE, 2)).cooldown(2).maxRepeat(1),
                        MoveDef.of("last_chapter", "终章·塔倾", 9, Effect.damage(10, 3), Effect.block(10), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("终焉").phase2()
                )));

        // ===================== Act IV · 落款 =====================
        add(new EnemyDef("seal_beetle", "印虫", "印",
                "它住在印章的凹字里，替所有没署名的东西盖章。被盖中的，就算写完了。",
                EnemyDef.Tier.NORMAL, 3, 58, 66,
                Arrays.asList(
                        MoveDef.of("stamp", "落印", 10, Effect.damage(14), Effect.apply(Status.VULNERABLE, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("vermilion", "硃泥", 8, Effect.damage(9, 2)).maxRepeat(1),
                        MoveDef.of("hide_seal", "藏印", 7, Effect.block(16)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("sip", "吮墨", 6, Effect.heal(7), Effect.power(Status.STRENGTH, 2)).cooldown(3).maxRepeat(1).intent("蓄力")
                )));

        add(new EnemyDef("rubric_hand", "朱笔签手", "签",
                "一只没有身体的手，替落款人试笔。它在你身上练签名，练成的都被收走了。",
                EnemyDef.Tier.NORMAL, 3, 60, 68,
                Arrays.asList(
                        MoveDef.of("sign", "署名", 10, Effect.damage(16)).maxRepeat(1),
                        MoveDef.of("cross_out", "划名", 8, Effect.damage(12), Effect.apply(Status.WEAK, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("hold_brush", "顿笔", 7, Effect.block(14), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("蓄力"),
                        MoveDef.of("flourish", "飞白", 6, Effect.damage(6, 2), Effect.apply(Status.FRAIL, 2)).cooldown(1).maxRepeat(1)
                )));

        add(new EnemyDef("clay_warden", "封泥卫", "泥",
                "封泥早已干裂，里面的人还活着。他守着最后一道封口，不肯让它开。",
                EnemyDef.Tier.NORMAL, 3, 64, 70,
                Arrays.asList(
                        MoveDef.of("smother", "封口", 9, Effect.damage(13), Effect.apply(Status.FRAIL, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("press_seal", "压印", 8, Effect.damage(20)).maxRepeat(1),
                        MoveDef.of("mud_wall", "泥墙", 7, Effect.block(20), Effect.heal(5)).cooldown(2).maxRepeat(1).intent("防御"),
                        MoveDef.of("harden", "硬化", 6, Effect.block(8), Effect.power(Status.STRENGTH, 3)).cooldown(3).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("ghost_writer", "代笔", "代",
                "它借你的手写完你的结局。字迹漂亮得让你想签下去，签完就不必再读。",
                EnemyDef.Tier.NORMAL, 3, 56, 64,
                Arrays.asList(
                        MoveDef.of("forge_hand", "仿笔", 10, Effect.damage(15), Effect.apply(Status.VULNERABLE, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("scrawl", "狂草", 8, Effect.damage(8, 3)).maxRepeat(1),
                        MoveDef.of("paperweight", "镇纸", 7, Effect.block(15), Effect.power(Status.STRENGTH, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("borrow_form", "借形", 6, Effect.power(Status.STRENGTH, 3)).cooldown(2).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("dry_ink_monk", "枯墨僧", "枯",
                "他把自己体内的墨一点点挤干，剩下的部分轻得能浮在纸上。",
                EnemyDef.Tier.NORMAL, 3, 60, 68,
                Arrays.asList(
                        MoveDef.of("dry_palm", "枯掌", 10, Effect.damage(14)).maxRepeat(1),
                        MoveDef.of("scrape", "刮墨", 8, Effect.damage(6, 3), Effect.loseHp(3)).cooldown(1).maxRepeat(1),
                        MoveDef.of("fast", "断食", 6, Effect.block(12), Effect.heal(9)).cooldown(3).maxRepeat(1),
                        MoveDef.of("crack", "皲裂", 7, Effect.apply(Status.VULNERABLE, 2), Effect.apply(Status.WEAK, 1)).cooldown(2).maxRepeat(1)
                )));

        add(new EnemyDef("terminal_reader", "最后读者", "读",
                "他读到了不该读的一行，于是整个人变成一句停不下来的句子。",
                EnemyDef.Tier.NORMAL, 3, 62, 70,
                Arrays.asList(
                        MoveDef.of("recite", "诵读", 10, Effect.damage(17)).maxRepeat(1),
                        MoveDef.of("turn_page", "翻页", 8, Effect.damage(9, 2), Effect.apply(Status.WEAK, 1)).cooldown(1).maxRepeat(1),
                        MoveDef.of("mark_page", "夹页", 6, Effect.block(18), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("强化"),
                        MoveDef.of("reread", "重读", 6, Effect.heal(10), Effect.power(Status.REGEN, 2)).cooldown(3).maxRepeat(1).intent("自愈")
                )));

        add(new EnemyDef("blank_king", "空白之王", "空",
                "它身上一个字也没有，却让所有靠近的人想起自己漏写的那一句。",
                EnemyDef.Tier.NORMAL, 3, 52, 58,
                Arrays.asList(
                        MoveDef.of("empty_page", "空页", 8, Effect.damage(10)),
                        MoveDef.of("leave_blank", "留白", 6, Effect.block(15)).cooldown(1).maxRepeat(1).intent("防御"),
                        MoveDef.of("erase", "抹去", 5, Effect.damage(6, 3)).cooldown(2).maxRepeat(1)
                )));

        add(new EnemyDef("last_drop", "末滴", "滴",
                "从笔尖悬了太久的一滴墨，落地之前已经想好了要落在哪里。",
                EnemyDef.Tier.NORMAL, 3, 46, 52,
                Arrays.asList(
                        MoveDef.of("drip", "滴落", 9, Effect.damage(7), Effect.apply(Status.POISON, 2)),
                        MoveDef.of("seep", "渗开", 6, Effect.apply(Status.POISON, 3)).cooldown(1).maxRepeat(1),
                        MoveDef.of("coagulate", "凝墨", 5, Effect.damage(16)).cooldown(2).maxRepeat(1)
                )));

        add(new EnemyDef("signatory", "落款人", "款",
                "他签下的每一个名字都会成真，包括你的死期。落款处空着，是留给你的。",
                EnemyDef.Tier.ELITE, 3, 122, 134,
                Arrays.asList(
                        MoveDef.of("seal_name", "钤名", 10, Effect.damage(20)).maxRepeat(1),
                        MoveDef.of("red_brush", "朱笔", 8, Effect.damage(12), Effect.apply(Status.VULNERABLE, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("desk", "伏案", 7, Effect.block(22), Effect.heal(6)).cooldown(2).maxRepeat(1).intent("防御"),
                        MoveDef.of("grind_ink", "研墨", 6, Effect.block(10), Effect.power(Status.STRENGTH, 3)).cooldown(3).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("omnibus_binder", "合订者", "订",
                "他把所有版本的你都订进同一册，厚得翻不开。他说，这样你就不会散。",
                EnemyDef.Tier.ELITE, 3, 120, 132,
                Arrays.asList(
                        MoveDef.of("spine_crush", "压脊", 10, Effect.damage(19)).maxRepeat(1),
                        MoveDef.of("stitch", "穿线", 8, Effect.damage(9, 2), Effect.apply(Status.FRAIL, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("fold_in", "折入", 6, Effect.apply(Status.SHACKLE, 2), Effect.apply(Status.VULNERABLE, 2)).cooldown(2).maxRepeat(1),
                        MoveDef.of("gather", "集册", 6, Effect.block(22), Effect.power(Status.STRENGTH, 3)).cooldown(3).maxRepeat(1).intent("强化")
                )));

        add(new EnemyDef("first_seal", "元印", "元",
                "所有印章都是它盖出来的。它盖下的第一个名字，是这座塔自己的名字。",
                EnemyDef.Tier.ELITE, 3, 124, 138,
                Arrays.asList(
                        MoveDef.of("imprint", "初印", 10, Effect.damage(22)).maxRepeat(1),
                        MoveDef.of("bleed_ink", "渗墨", 8, Effect.damage(10, 2), Effect.apply(Status.FRAIL, 2)).cooldown(1).maxRepeat(1),
                        MoveDef.of("enshrine", "供奉", 6, Effect.block(25), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("防御"),
                        MoveDef.of("decree", "敕令", 7, Effect.apply(Status.VULNERABLE, 3), Effect.apply(Status.WEAK, 2)).cooldown(2).maxRepeat(1)
                )));

        add(new EnemyDef("the_ink_itself", "墨之本源", "源",
                "塔只写了一页，落款处一直是空的。现在它要你把手按上去，替它签完。",
                EnemyDef.Tier.BOSS, 3, 330, 376,
                Arrays.asList(
                        MoveDef.of("first_stroke", "起笔", 10, Effect.damage(22)).maxRepeat(1),
                        MoveDef.of("ink_flood", "墨漫", 8, Effect.damage(11, 3)).cooldown(1).maxRepeat(1),
                        MoveDef.of("seal_all", "总封", 7, Effect.block(30), Effect.heal(12)).cooldown(3).maxRepeat(1).intent("防御"),
                        MoveDef.of("sign", "落款", 7, Effect.apply(Status.VULNERABLE, 3), Effect.apply(Status.WEAK, 2), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("强化"),
                        MoveDef.of("final_page", "终页·落款", 9, Effect.damage(11, 3), Effect.block(15), Effect.power(Status.STRENGTH, 2)).cooldown(2).maxRepeat(1).intent("终焉").phase2()
                )));
    }

    private EnemyLibrary() {}

    private static void add(EnemyDef def) {
        ENEMIES.put(def.id, def);
        int act = clamp(def.act);
        if (def.tier == EnemyDef.Tier.NORMAL) NORMAL.get(act).add(def);
        else if (def.tier == EnemyDef.Tier.ELITE) ELITE.get(act).add(def);
        else BOSS[act] = def;
    }

    private static int clamp(int act) {
        if (act < 0) return 0;
        if (act > 3) return 3;
        return act;
    }

    public static List<EnemyDef> normalPool(int act) {
        return new ArrayList<>(NORMAL.get(clamp(act)));
    }

    public static List<EnemyDef> elitePool(int act) {
        return new ArrayList<>(ELITE.get(clamp(act)));
    }

    public static EnemyDef boss(int act) {
        return BOSS[clamp(act)];
    }

    public static EnemyDef get(String id) {
        if (id == null) return null;
        return ENEMIES.get(id);
    }

    public static List<EnemyDef> all() {
        return new ArrayList<>(ENEMIES.values());
    }

    /**
     * Normal encounter for one node. Bosses are never included. Group size grows with the act;
     * deeper floors put the toughest available enemy in the first slot.
     */
    public static List<EnemyState> rollEncounter(int act, Random rng, int floor) {
        Random r = rng == null ? new Random() : rng;
        int a = clamp(act);
        int count;
        if (a == 0) {
            count = r.nextDouble() < 0.20 ? 2 : 1;
        } else if (a == 1) {
            double roll = r.nextDouble();
            count = roll < 0.20 ? 3 : (roll < 0.70 ? 2 : 1);
        } else if (a == 2) {
            count = r.nextDouble() < 0.45 ? 2 : 3;
        } else {
            count = r.nextDouble() < 0.65 ? 3 : 2;
        }
        if (count > 3) count = 3;
        List<EnemyDef> pool = new ArrayList<>(NORMAL.get(a));
        Collections.shuffle(pool, r);
        if (floor >= 8 && !pool.isEmpty()) {
            EnemyDef toughest = pool.get(0);
            for (EnemyDef def : pool) if (def.hpMax > toughest.hpMax) toughest = def;
            pool.remove(toughest);
            pool.add(0, toughest);
        }
        ArrayList<EnemyState> out = new ArrayList<>();
        for (EnemyDef def : pool) {
            if (out.size() >= count) break;
            out.add(new EnemyState(def, def.hpMin, r));
        }
        return out;
    }

    /** Elite encounters are always a single enemy. */
    public static List<EnemyState> rollElite(int act, Random rng) {
        Random r = rng == null ? new Random() : rng;
        List<EnemyDef> pool = elitePool(act);
        ArrayList<EnemyState> out = new ArrayList<>();
        if (pool.isEmpty()) return out;
        EnemyDef def = pool.get(r.nextInt(pool.size()));
        out.add(new EnemyState(def, def.hpMin, r));
        return out;
    }
}
