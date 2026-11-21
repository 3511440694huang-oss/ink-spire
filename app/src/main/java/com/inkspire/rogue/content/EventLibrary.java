package com.inkspire.rogue.content;

import com.inkspire.rogue.core.PotionState;
import com.inkspire.rogue.core.RunState;
import com.inkspire.rogue.model.CardDef;
import com.inkspire.rogue.model.CardInstance;
import com.inkspire.rogue.model.PotionDef;
import com.inkspire.rogue.model.Rarity;
import com.inkspire.rogue.model.RelicDef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Small text events. Every outcome is immediate and readable in one screen. */
public final class EventLibrary {
    public static final class Choice {
        public final String label;
        public final String hint;
        public Choice(String label, String hint) { this.label = label; this.hint = hint; }
    }

    public static final class EventDef {
        public final String id, title, glyph, body;
        /** -1 = 任何幕都能出；0..3 = 只在第 N 幕出现。 */
        public final int act;
        public final List<Choice> choices;
        public EventDef(String id, String title, String glyph, String body, Choice... choices) {
            this(id, -1, title, glyph, body, choices);
        }
        private EventDef(String id, int act, String title, String glyph, String body, Choice... choices) {
            this.id = id; this.act = act; this.title = title; this.glyph = glyph; this.body = body;
            this.choices = Collections.unmodifiableList(Arrays.asList(choices));
        }
        /** 带幕锁的构造入口：act = -1 通用，0..3 绑定对应幕。 */
        public static EventDef of(String id, int act, String title, String glyph, String body, Choice... choices) {
            return new EventDef(id, act, title, glyph, body, choices);
        }
    }

    private static final List<EventDef> EVENTS = new ArrayList<>();

    static {
        /* ============ 通用事件（act = -1） ============ */
        EVENTS.add(new EventDef("ink_well", "墨井", "井",
                "走廊尽头有一口井。井里没有水，只有缓慢翻涌的黑色墨汁。你听见下面有人用你的声音念书。",
                new Choice("饮下一口", "回复 15 生命，也许还能多些什么。"),
                new Choice("投入 30 金币", "换一件遗物。"),
                new Choice("转身离开", "什么也不发生。")));
        EVENTS.add(new EventDef("blank_page", "空白页", "页",
                "一页真正的空白横在路中央。它没有字，却比任何一页都重。你可以在上面留下名字，或者把它撕成钱。",
                new Choice("写下名字", "最大生命 +6，失去 7 生命。"),
                new Choice("撕成碎片", "获得 80 金币。"),
                new Choice("跨过去", "什么也不发生。")));
        EVENTS.add(new EventDef("night_watch", "守夜人", "灯",
                "一个披着旧袍的人坐在灯下，替所有未写完的故事守夜。他抬起头，认出了你书页上的折角。",
                new Choice("付他 60 金币", "随机升级一张牌。"),
                new Choice("陪他坐到天亮", "回复 10 生命，获得一瓶药水。"),
                new Choice("谢绝", "什么也不发生。")));
        EVENTS.add(new EventDef("ink_merchant", "墨贩", "贩",
                "一个没有脸的商人推着小车。车上摆着装在墨水瓶里的药水，还有一叠用头发装订的卡牌。",
                new Choice("买一瓶药水（40 金币）", "获得随机药水。"),
                new Choice("买一张卡（65 金币）", "获得一张随机稀有卡。"),
                new Choice("离开", "什么也不发生。")));
        EVENTS.add(new EventDef("echo", "塔中回声", "回",
                "你喊了一声。回声比你多喊了一声。第二声里，夹着一个不属于你的名字。你停下，它也停下。",
                new Choice("回应它", "失去 10 生命，获得一件遗物。"),
                new Choice("保持沉默", "获得 50 金币。")));
        EVENTS.add(new EventDef("bookcase", "倾斜书柜", "柜",
                "一整排书柜向走廊内侧倾斜，像随时会倒下的多米诺。最上层有一只上了锁的小盒。锁孔里塞着一小片纸。",
                new Choice("推开书柜", "失去 8 生命，获得 120 金币。"),
                new Choice("只拿最下面一本", "获得一张随机普通卡。"),
                new Choice("离开", "什么也不发生。")));
        EVENTS.add(new EventDef("pilgrim", "朝圣者", "圣",
                "一个背着空书架的朝圣者向你伸手。他说自己已经走到了最后一页，却找不到装订线。他说话时，书架上落下一层灰。",
                new Choice("施舍 50 金币", "最大生命 +8。"),
                new Choice("交换一页", "随机移除一张牌，获得一张随机稀有牌。"),
                new Choice("摇头离开", "什么也不发生。")));
        EVENTS.add(new EventDef("mirror", "墨镜", "镜",
                "墙上挂着一面用墨涂黑的镜子。镜中的人比你慢半拍，正在翻一本你从未拥有的牌组。镜面比墙更冷。",
                new Choice("凝视镜面", "复制牌组中随机一张牌。"),
                new Choice("打碎镜子", "获得 75 金币，失去 5 生命。"),
                new Choice("离开", "什么也不发生。")));
        EVENTS.add(new EventDef("bell", "静默钟", "钟",
                "一口没有钟舌的钟悬在梁上。钟壁上刻着：敲响它的人，会听见自己尚未说出口的话。钟口朝向地面。",
                new Choice("敲响", "回复 25 生命。"),
                new Choice("贴近聆听", "最大生命 +2，获得一瓶药水。"),
                new Choice("离开", "什么也不发生。")));
        EVENTS.add(new EventDef("seal", "旧印章", "印",
                "抽屉里躺着一枚干涸的印章，印面是这座塔的剖面。你把手按上去，纸面开始发烫。油墨味像铁锈。",
                new Choice("盖章", "失去 15 生命，获得一件遗物。"),
                new Choice("熔掉印章", "获得 150 金币。")));

        EVENTS.add(new EventDef("ink_moth", "墨蛾", "蛾",
                "灯下停着一只蛾，翅膀上全是未干的墨。它不飞，只慢慢开合，每开合一次就落下一小片黑。",
                new Choice("伸出手背", "升级一张牌，失去 4 生命"),
                new Choice("刮下墨迹", "获得 45 金币。"),
                new Choice("赶走它", "什么也不发生。")));
        EVENTS.add(new EventDef("debt", "收账人", "账",
                "一个提账本的人拦住你，说你欠了塔一页纸。他翻开账本，上面确实是你的笔迹，你却记不起写过。",
                new Choice("让他取一张牌", "移除一张牌，获得 70 金币与状态牌"),
                new Choice("付 90 金币清账", "获得一件遗物。"),
                new Choice("转身走开", "失去 6 生命，账照样记着")));
        EVENTS.add(new EventDef("scribe", "抄写员", "抄",
                "一个抄写员跪在走廊里，面前摊着与你牌组一模一样的牌。他抄得很快，笔尖从不蘸墨。腕上没有影子。",
                new Choice("递过去一张", "复制一张牌，牌组多一张诅咒"),
                new Choice("付 60 金币", "随机升级一张牌。"),
                new Choice("抽走那叠纸", "失去 5 生命，获得 50 金币")));
        EVENTS.add(new EventDef("dice", "骨骰", "骰",
                "地上散着三枚用指骨磨成的骰子，没有对手，也没有赌桌。骰子自己滚了半圈，停在三点上，等你下注。",
                new Choice("押上 60 金币", "赢150金币，输则失去6生命"),
                new Choice("押上一滴血", "赢则得遗物，输则失去10生命"),
                new Choice("离开赌桌", "什么也不发生。")));
        EVENTS.add(new EventDef("ink_spring", "墨泉", "泉",
                "石缝里渗出一小股墨，聚成刚好没过手腕的浅洼。水面平得像另一张纸，映出的你没有伤口。",
                new Choice("饮下三口", "回复 20 生命，牌组多一张诅咒"),
                new Choice("以泉洗牌", "升级一张牌，失去 5 生命"),
                new Choice("绕过它", "什么也不发生。")));
        EVENTS.add(new EventDef("reader", "未眠读者", "读",
                "有人坐在阶梯上读书，书脊写着你的名字。他抬头看你一眼，把书翻到有折角的那一页，又低下去。",
                new Choice("取走书签", "获得一件遗物，牌组多一张诅咒"),
                new Choice("坐到他旁边", "回复 12 生命，获得一瓶药水"),
                new Choice("合上他的书", "移除一张牌，失去 4 生命")));
        EVENTS.add(new EventDef("binder", "装订匠", "订",
                "走廊尽头有个装订匠在缝书，针脚密得看不见线。他抬眼问你，要不要把散着的几页缝紧一点。",
                new Choice("缝紧牌组", "升级两张牌，最大生命 -4"),
                new Choice("缝一针伤口", "回复 15 生命，移除一张牌"),
                new Choice("谢绝", "什么也不发生。")));
        EVENTS.add(new EventDef("candle", "短烛", "烛",
                "墙龛里剩半截蜡烛，火苗贴着地面烧，把影子拉得很长。影子比你先动，抬手扶了一下灯芯。",
                new Choice("吹灭它", "失去 5 生命，获得 60 金币"),
                new Choice("添一根灯芯", "最大生命 +4，回复 4 生命"),
                new Choice("对着火苗许愿", "失去 3 生命，获得一瓶药水")));

        /* ============ 幕锁事件（act = 0..3） ============ */
        EVENTS.add(EventDef.of("blotted_name", 0, "抹名", "抹",
                "回廊的墙上钉着一册名册，纸面被墨反复涂过。仍有几个名字露在外面，笔画微微抽动，像在呼吸。",
                new Choice("划去一个名字", "随机移除一张牌，失去 4 生命"),
                new Choice("添上自己的名字", "最大生命 +5，牌组多一张诅咒"),
                new Choice("合上名册", "什么也不发生。")));
        EVENTS.add(EventDef.of("hush_room", 1, "无声室", "默",
                "书房中央摆着一张空桌，桌上摊开一本读到一半的书。你开口时声音没有传出去，纸面却动了一下。",
                new Choice("补上一句", "获得稀有牌，牌组多一张诅咒"),
                new Choice("把书合上", "回复 12 生命。"),
                new Choice("带走这本书", "失去 8 生命，获得 90 金币")));
        EVENTS.add(EventDef.of("ledge", 2, "塔尖的风", "风",
                "塔尖只剩一圈石沿，风从四个方向同时吹来，把纸页往上托。往下看，回廊与书库缩成两行小字。",
                new Choice("纵身一跃", "或得遗物，或失去 15 生命"),
                new Choice("沿石沿绕行", "失去 6 生命，获得 100 金币"),
                new Choice("退回塔内", "什么也不发生。")));
        EVENTS.add(EventDef.of("last_sign", 3, "落款", "款",
                "最后一页摊在桌上，留白处已经画好一条横线。墨与笔都备着，纸的边角微微发烫，等你签下名字。",
                new Choice("签下名字", "最大生命 +8，牌组多两张诅咒"),
                new Choice("折断这支笔", "获得 130 金币，随机移除一张牌"),
                new Choice("留白不写", "回复 20 生命。")));
    }

    private EventLibrary() {}

    public static EventDef random(Random rng) {
        if (EVENTS.isEmpty()) return null;
        Random r = rng == null ? new Random() : rng;
        return EVENTS.get(r.nextInt(EVENTS.size()));
    }

    /**
     * 按幕筛选、本局不重复的事件抽取。永远不返回 null。
     * 先抽「幕匹配且未看过」，再放宽到「未看过」，最后才退回任意一个。
     */
    public static EventDef random(RunState run) {
        if (EVENTS.isEmpty()) return null;
        if (run == null) return random((Random) null);
        Random rng = run.rng == null ? new Random() : run.rng;
        ArrayList<EventDef> pool = new ArrayList<>();
        for (EventDef e : EVENTS) {
            if ((e.act == -1 || e.act == run.act) && !run.seenEvents.contains(e.id)) pool.add(e);
        }
        if (pool.isEmpty()) {
            for (EventDef e : EVENTS) if (!run.seenEvents.contains(e.id)) pool.add(e);
        }
        EventDef pick = pool.isEmpty()
                ? EVENTS.get(rng.nextInt(EVENTS.size()))
                : pool.get(rng.nextInt(pool.size()));
        run.seenEvents.add(pick.id);
        return pick;
    }

    public static List<EventDef> all() { return new ArrayList<>(EVENTS); }

    public static String resolve(RunState run, EventDef event, int index) {
        if (event == null || index < 0 || index >= event.choices.size()) return "";
        Random rng = run.rng;
        switch (event.id) {
            case "ink_well":
                if (index == 0) {
                    run.hp = Math.min(run.maxHp, run.hp + 15);
                    if (rng.nextDouble() < 0.3) { run.maxHp += 3; run.hp += 3; return "井水像温热的墨。你回复了 15 生命，最大生命 +3。"; }
                    return "井水像温热的墨。你回复了 15 生命。";
                }
                if (index == 1) {
                    if (run.gold < 30) return "你摸了摸口袋，金币不够。井底传来一声轻笑。";
                    run.gold -= 30;
                    RelicDef r = RelicLibrary.random(rng, run.relics);
                    if (r == null) { run.gold += 30; return "井底什么也没有。"; }
                    run.addRelic(r.id);
                    return "金币沉下去，浮上来一枚「" + r.name + "」。";
                }
                return "你离开了墨井。";
            case "blank_page":
                if (index == 0) {
                    run.maxHp += 6; run.hp = Math.max(1, run.hp - 7 + 6);
                    return "名字渗进纸里。最大生命 +6，失去 7 生命。";
                }
                if (index == 1) { run.gainGold(80); return "空白页被撕成碎片，换来了 80 金币。"; }
                return "你跨过空白页。";
            case "night_watch":
                if (index == 0) {
                    if (run.gold < 60) return "守夜人看了看你的口袋，把灯芯拨暗了。";
                    run.gold -= 60;
                    CardInstance up = upgradeRandom(run);
                    return up == null ? "守夜人摇了摇头：没有可升级的牌。" : "「" + up.name() + "」被重新誊写了一遍。";
                }
                if (index == 1) {
                    run.hp = Math.min(run.maxHp, run.hp + 10);
                    PotionDef p = PotionLibrary.random(rng);
                    run.addPotion(p.id);
                    return "你陪他坐到天亮。回复 10 生命，获得「" + p.name + "」。";
                }
                return "你谢绝了守夜人。";
            case "ink_merchant":
                if (index == 0) {
                    if (run.gold < 40) return "商人把药水收了回去。";
                    run.gold -= 40;
                    PotionDef p = PotionLibrary.random(rng);
                    if (!run.addPotion(p.id)) { run.gold += 40; return "你的药水袋已经满了。"; }
                    return "你买下了「" + p.name + "」。";
                }
                if (index == 1) {
                    if (run.gold < 65) return "商人把卡牌收了回去。";
                    run.gold -= 65;
                    CardDef c = rareCard(rng);
                    run.deck.add(new CardInstance(c));
                    return "你买下了「" + c.name + "」。";
                }
                return "你离开了墨贩的小车。";
            case "echo":
                if (index == 0) {
                    run.hp = Math.max(1, run.hp - 10);
                    RelicDef r = RelicLibrary.random(rng, run.relics);
                    if (r == null) return "回声撞在墙上，什么也没留下。";
                    run.addRelic(r.id);
                    return "回声替你回答了。失去 10 生命，获得「" + r.name + "」。";
                }
                run.gainGold(50);
                return "你闭上嘴，金币落进衣袋。";
            case "bookcase":
                if (index == 0) {
                    run.hp = Math.max(1, run.hp - 8);
                    run.gainGold(120);
                    return "书柜轰然倒下。你被划伤了，但捡到了 120 金币。";
                }
                if (index == 1) {
                    CardDef c = commonCard(rng);
                    run.deck.add(new CardInstance(c));
                    return "你抽出一本薄册，得到「" + c.name + "」。";
                }
                return "你从书柜边绕了过去。";
            case "pilgrim":
                if (index == 0) {
                    if (run.gold < 50) return "朝圣者看了看你，没有收下空手。";
                    run.gold -= 50; run.maxHp += 8; run.hp += 8;
                    return "朝圣者在你额头上画了一道线。最大生命 +8。";
                }
                if (index == 1) {
                    if (run.deck.size() <= 1) return "你的牌组太薄，朝圣者拒绝交换。";
                    CardInstance removed = run.deck.remove(rng.nextInt(run.deck.size()));
                    CardDef c = rareCard(rng);
                    run.deck.add(new CardInstance(c));
                    return "「" + removed.name() + "」被换成了「" + c.name + "」。";
                }
                return "你摇了摇头。";
            case "mirror":
                if (index == 0) {
                    CardInstance src = run.deck.get(rng.nextInt(run.deck.size()));
                    run.deck.add(src.copy());
                    return "镜中伸出一只手，把「" + src.name() + "」递给了你。";
                }
                if (index == 1) {
                    run.gold += 75; run.hp = Math.max(1, run.hp - 5);
                    return "镜子碎了。获得 75 金币，失去 5 生命。";
                }
                return "你没有看它第二眼。";
            case "bell":
                if (index == 0) { run.hp = Math.min(run.maxHp, run.hp + 25); return "钟没有声音，但你的伤口合上了。回复 25 生命。"; }
                if (index == 1) {
                    run.maxHp += 2; run.hp += 2;
                    PotionDef p = PotionLibrary.random(rng);
                    run.addPotion(p.id);
                    return "你听见一句很轻的话。最大生命 +2，获得「" + p.name + "」。";
                }
                return "你离开了钟。";
            case "seal":
                if (index == 0) {
                    run.hp = Math.max(1, run.hp - 15);
                    RelicDef r = RelicLibrary.random(rng, run.relics);
                    if (r == null) return "印章烫了你一下，什么也没留下。";
                    run.addRelic(r.id);
                    return "纸面留下一道塔形焦痕。失去 15 生命，获得「" + r.name + "」。";
                }
                run.gainGold(150);
                return "印章在火里化成一摊黑。获得 150 金币。";

            /* ---------------- 通用：墨蛾 ---------------- */
            case "ink_moth":
                if (index == 0) {
                    if (!canPayHp(run, 4)) return "你把手缩了回来，墨蛾在灯下继续开合。";
                    run.loseHp(4);
                    CardInstance mbUp = run.upgradeRandomCard();
                    return mbUp == null
                            ? "墨渗进伤口。失去 4 生命，但你的牌组已经无可誊写。"
                            : "墨渗进伤口，笔迹变深。「" + mbUp.name() + "」被升级了，失去 4 生命。";
                }
                if (index == 1) {
                    run.gainGold(45);
                    return "你刮下翅上未干的墨，卖给说不清是谁的买家。获得 45 金币。";
                }
                return "你挥手赶开它。灯焰晃了一下，什么也没有发生。";

            /* ---------------- 通用：收账人 ---------------- */
            case "debt":
                if (index == 0) {
                    CardInstance debtGone = run.removeRandomCard();
                    run.gainGold(70);
                    CardDef debtSlip = run.addRandomStatusCard();
                    String slip = debtSlip == null ? "" : "，外加一张「" + debtSlip.name + "」";
                    return debtGone == null
                            ? "账本上撕下一角，换成了 70 金币" + slip + "。"
                            : "「" + debtGone.name() + "」被抵给了账本，换来 70 金币" + slip + "。";
                }
                if (index == 1) {
                    if (run.gold < 90) return "他合上账本：钱不够，账不会清。";
                    run.gold -= 90;
                    RelicDef debtRelic = run.randomRelicDef();
                    if (debtRelic == null) { run.gold += 90; return "他翻了很久，也没翻出能抵账的东西。"; }
                    run.addRelic(debtRelic.id);
                    return "账页被撕掉一页，你手里多了一枚「" + debtRelic.name + "」。";
                }
                if (!canPayHp(run, 6)) return "你贴着墙绕过去，账本在身后合上。";
                run.loseHp(6);
                return "你转身就走。账本自己翻了一页，扣掉 6 生命作为利息。";

            /* ---------------- 通用：抄写员 ---------------- */
            case "scribe":
                if (index == 0) {
                    CardInstance scribed = run.duplicateRandomCard();
                    if (scribed == null) return "他摊开手：你的牌组是空的。";
                    CardDef scribeCurse = run.addRandomCurse();
                    return scribeCurse == null
                            ? "「" + scribed.name() + "」被抄了一份，笔画略歪。"
                            : "「" + scribed.name() + "」被抄了一份，纸背多出「" + scribeCurse.name + "」。";
                }
                if (index == 1) {
                    if (run.gold < 60) return "抄写员伸出手，又收了回去。";
                    run.gold -= 60;
                    CardInstance scribeUp = run.upgradeRandomCard();
                    return scribeUp == null
                            ? "他把金币收进袖子，你的牌已经没什么可改。"
                            : "他替你重抄了「" + scribeUp.name() + "」。";
                }
                if (!canPayHp(run, 5)) return "你没有伸手去抽那叠纸。";
                run.loseHp(5);
                run.gainGold(50);
                return "你抽走那叠纸，他的指甲划破你的手背。失去 5 生命，获得 50 金币。";

            /* ---------------- 通用：骨骰 ---------------- */
            case "dice":
                if (index == 0) {
                    if (run.gold < 60) return "骰子滚了半圈，停在你空着的手边。";
                    if (!canPayHp(run, 6)) return "你摸了摸自己的脉搏，把金币收了回去。";
                    run.gold -= 60;
                    if (rng.nextDouble() < 0.5) {
                        run.gainGold(150);
                        return "三枚骰子同时停在六点。获得 150 金币，押下的 60 已经不算什么。";
                    }
                    run.loseHp(6);
                    return "骰子停在一点。押的 60 金币没了，你还失去 6 生命。";
                }
                if (index == 1) {
                    if (!canPayHp(run, 10)) return "你看着自己的手腕，把袖子放了下来。";
                    if (rng.nextDouble() < 0.5) {
                        RelicDef diceRelic = run.randomRelicDef();
                        if (diceRelic == null) return "骰子停在空面上，什么也没有。";
                        run.addRelic(diceRelic.id);
                        return "骰子停在你想要的那一面，手里多了一枚「" + diceRelic.name + "」。";
                    }
                    run.loseHp(10);
                    return "骰子停在一点。失去 10 生命，血落进骰子的凹坑里。";
                }
                return "你离开了赌桌。骰子在身后自己滚了一圈。";

            /* ---------------- 通用：墨泉 ---------------- */
            case "ink_spring":
                if (index == 0) {
                    run.heal(20);
                    CardDef springCurse = run.addRandomCurse();
                    return springCurse == null
                            ? "泉水像温热的墨。回复 20 生命。"
                            : "泉水像温热的墨。回复 20 生命，牌组里多了一张「" + springCurse.name + "」。";
                }
                if (index == 1) {
                    if (!canPayHp(run, 5)) return "你把手从水面上方收了回来。";
                    run.loseHp(5);
                    CardInstance springUp = run.upgradeRandomCard();
                    return springUp == null
                            ? "水面映不出你的牌。失去 5 生命，没有可升级的牌。"
                            : "你在泉里洗了一遍牌。「" + springUp.name() + "」被升级了，失去 5 生命。";
                }
                return "你绕过泉眼，鞋底还是沾了一点黑。";

            /* ---------------- 通用：未眠读者 ---------------- */
            case "reader":
                if (index == 0) {
                    RelicDef readerRelic = run.randomRelicDef();
                    if (readerRelic == null) return "书签从他指间滑落，什么也没带走。";
                    run.addRelic(readerRelic.id);
                    CardDef readerCurse = run.addRandomCurse();
                    return readerCurse == null
                            ? "你取走书签，得到「" + readerRelic.name + "」。"
                            : "你取走书签，得到「" + readerRelic.name + "」，牌组多了一张「" + readerCurse.name + "」。";
                }
                if (index == 1) {
                    run.heal(12);
                    boolean gotPotion = run.addRandomPotion();
                    return gotPotion
                            ? "你在他旁边坐下，读到同一行。回复 12 生命，获得一瓶药水。"
                            : "你在他旁边坐下，读到同一行。回复 12 生命，药水袋却已经满了。";
                }
                if (!canPayHp(run, 4)) return "你的手停在他书脊上方，没有翻下去。";
                run.loseHp(4);
                CardInstance readerGone = run.removeRandomCard();
                return readerGone == null
                            ? "你把书合上，纸边割破手指。失去 4 生命。"
                            : "你把书合上，纸边割破手指。「" + readerGone.name() + "」从牌组里消失了。";

            /* ---------------- 通用：装订匠 ---------------- */
            case "binder":
                if (index == 0) {
                    if (run.maxHp <= 5) return "装订匠看了看你：纸太薄，缝不住。";
                    run.addMaxHp(-4, false);
                    int stitched = 0;
                    if (run.upgradeRandomCard() != null) stitched++;
                    if (run.upgradeRandomCard() != null) stitched++;
                    return stitched == 0
                            ? "针脚收紧，你的呼吸也紧了。最大生命 -4。"
                            : "散页被缝紧，最大生命 -4，" + stitched + " 张牌被重新誊写。";
                }
                if (index == 1) {
                    CardInstance unbound = run.removeRandomCard();
                    if (unbound == null) return "他摇了摇头：没有可拆的线。";
                    run.heal(15);
                    return "他拆下「" + unbound.name() + "」补在你的伤口上。回复 15 生命。";
                }
                return "你谢过装订匠，没有把牌组交出去。";

            /* ---------------- 通用：短烛 ---------------- */
            case "candle":
                if (index == 0) {
                    if (!canPayHp(run, 5)) return "火苗贴着手背烧过去，你把手收了回来。";
                    run.loseHp(5);
                    run.gainGold(60);
                    return "你吹灭了它，墙龛后掉出一把旧币。失去 5 生命，获得 60 金币。";
                }
                if (index == 1) {
                    if (run.gold < 12) return "你摸遍口袋，连一根灯芯也换不起。";
                    run.gold -= 12;
                    run.addMaxHp(4, true);
                    return "你添了一根新灯芯，火苗立起来。最大生命 +4，回复 4 生命。";
                }
                if (!canPayHp(run, 3)) return "你没有对火苗开口。";
                run.loseHp(3);
                boolean wishPotion = run.addRandomPotion();
                return wishPotion
                        ? "你对着火苗说了一句话。失去 3 生命，获得一瓶药水。"
                        : "你对着火苗说了一句话。失去 3 生命，药水袋却已经满了。";

            /* ---------------- 第一幕：抹名 ---------------- */
            case "blotted_name":
                if (index == 0) {
                    if (!canPayHp(run, 4)) return "你的手指停在名册上方，最后还是收了回去。";
                    run.loseHp(4);
                    CardInstance blotted = run.removeRandomCard();
                    return blotted == null
                            ? "名册空出一格，你的牌组却已无可划去。失去 4 生命。"
                            : "「" + blotted.name() + "」从名册上被划去。失去 4 生命。";
                }
                if (index == 1) {
                    run.addMaxHp(5, true);
                    CardDef nameCurse = run.addRandomCurse();
                    return nameCurse == null
                            ? "你的名字被墨吃进去。最大生命 +5。"
                            : "你的名字被墨吃进去。最大生命 +5，牌组里多了一张「" + nameCurse.name + "」。";
                }
                return "你合上名册，墙上的墨还在慢慢往下渗。";

            /* ---------------- 第二幕：无声室 ---------------- */
            case "hush_room":
                if (index == 0) {
                    CardDef hushCard = run.randomCard(Rarity.RARE);
                    if (hushCard == null) return "笔尖悬在纸上，最后什么也没落下。";
                    run.addCard(hushCard, false);
                    CardDef hushCurse = run.addRandomCurse();
                    return hushCurse == null
                            ? "你补上一句，纸上浮出「" + hushCard.name + "」。"
                            : "你补上一句，得到「" + hushCard.name + "」，纸角爬满「" + hushCurse.name + "」。";
                }
                if (index == 1) {
                    run.heal(12);
                    return "你合上书，房间终于有了一点回声。回复 12 生命。";
                }
                if (!canPayHp(run, 8)) return "书太重，你把它放回桌上。";
                run.loseHp(8);
                run.gainGold(90);
                return "你把书塞进怀里，书脊硌着肋骨。失去 8 生命，获得 90 金币。";

            /* ---------------- 第三幕：塔尖的风 ---------------- */
            case "ledge":
                if (index == 0) {
                    if (!canPayHp(run, 15)) return "风把纸页吹得猎猎作响，你贴着石沿没有动。";
                    if (rng.nextDouble() < 0.5) {
                        RelicDef ledgeRelic = run.randomRelicDef();
                        if (ledgeRelic != null) {
                            run.addRelic(ledgeRelic.id);
                            return "你落在对面，掌心里多了一枚「" + ledgeRelic.name + "」。";
                        }
                    }
                    run.loseHp(15);
                    return "你撞在石沿上，抓住一道裂缝爬了回来。失去 15 生命。";
                }
                if (index == 1) {
                    if (!canPayHp(run, 6)) return "石沿太窄，你没有踩上去。";
                    run.loseHp(6);
                    run.gainGold(100);
                    return "你贴着墙绕过去，风割开手背。失去 6 生命，获得 100 金币。";
                }
                return "你退回塔内，风在门外停了。";

            /* ---------------- 第四幕：落款 ---------------- */
            case "last_sign":
                if (index == 0) {
                    run.addMaxHp(8, true);
                    CardDef signCurseA = run.addRandomCurse();
                    CardDef signCurseB = run.addRandomCurse();
                    StringBuilder signText = new StringBuilder("名字落在横线上。最大生命 +8");
                    if (signCurseA != null) signText.append("，牌组多出「").append(signCurseA.name).append("」");
                    if (signCurseB != null) signText.append("与「").append(signCurseB.name).append("」");
                    return signText.append("。").toString();
                }
                if (index == 1) {
                    run.gainGold(130);
                    CardInstance signGone = run.removeRandomCard();
                    return signGone == null
                            ? "笔尖折断，墨溅在落款处。获得 130 金币。"
                            : "笔尖折断，墨溅在落款处。获得 130 金币，「" + signGone.name() + "」被抹去。";
                }
                run.heal(20);
                return "你让横线空着。纸慢慢凉下去，回复 20 生命。";

            default:
                return "";
        }
    }

    /** 扣血前先确认扛得住：扣完至少还剩 1 点生命。 */
    private static boolean canPayHp(RunState run, int cost) { return run.hp > cost + 1; }

    private static CardInstance upgradeRandom(RunState run) {
        ArrayList<CardInstance> pool = new ArrayList<>();
        for (CardInstance c : run.deck) if (!c.upgraded && c.def.upgraded() != null) pool.add(c);
        if (pool.isEmpty()) return null;
        CardInstance c = pool.get(run.rng.nextInt(pool.size()));
        c.upgraded = true;
        return c;
    }

    private static CardDef commonCard(Random rng) {
        while (true) {
            CardDef c = CardLibrary.randomReward(rng);
            if (c.rarity == Rarity.COMMON) return c;
        }
    }

    private static CardDef rareCard(Random rng) {
        while (true) {
            CardDef c = CardLibrary.randomReward(rng);
            if (c.rarity == Rarity.RARE) return c;
        }
    }
}
