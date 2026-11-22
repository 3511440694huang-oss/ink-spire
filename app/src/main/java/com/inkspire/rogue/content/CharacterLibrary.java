package com.inkspire.rogue.content;

import com.inkspire.rogue.model.CardInstance;

import java.util.ArrayList;
import java.util.List;

/** 可选角色。每个角色有自己的初始牌组、初始遗物、生命上限与卡池。 */
public final class CharacterLibrary {

    public static final class CharacterDef {
        public final String id;
        public final String name;
        public final String title;
        public final String glyph;
        /** 一句话气质。 */
        public final String blurb;
        /** 玩法关键词，用于选人界面。 */
        public final String playstyle;
        public final int maxHp;
        public final int gold;
        public final String starterRelic;

        CharacterDef(String id, String name, String title, String glyph,
                     String blurb, String playstyle, int maxHp, int gold, String starterRelic) {
            this.id = id; this.name = name; this.title = title; this.glyph = glyph;
            this.blurb = blurb; this.playstyle = playstyle;
            this.maxHp = maxHp; this.gold = gold; this.starterRelic = starterRelic;
        }

        public List<CardInstance> starterDeck() {
            return CardLibrary.starterDeck(id);
        }
    }

    private static final List<CharacterDef> ALL = new ArrayList<>();

    static {
        ALL.add(new CharacterDef("iron", "铁刻", "墨塔的装订工", "铁",
                "他把自己的名字刻在书脊上，刻得太深，就成了封面。",
                "力量 · 厚盾 · 消耗",
                80, 99, "burning_blood"));
        ALL.add(new CharacterDef("silk", "游丝", "缝页的裁线人", "丝",
                "一根线穿过所有页角，动一处，全书都跟着响。",
                "敏捷 · 虚弱 · 抽牌",
                78, 99, "jade_pendant"));
        ALL.add(new CharacterDef("ash", "灰烬", "烧掉半本书的人", "烬",
                "烧掉的那部分，才是真正属于你的。",
                "自伤 · 爆发 · 诅咒",
                84, 111, "ember_box"));
    }

    private CharacterLibrary() {}

    public static List<CharacterDef> all() { return new ArrayList<>(ALL); }

    public static CharacterDef get(String id) {
        if (id != null) {
            for (CharacterDef c : ALL) if (c.id.equals(id)) return c;
        }
        return ALL.get(0);
    }

    public static String defaultId() { return ALL.get(0).id; }
}
