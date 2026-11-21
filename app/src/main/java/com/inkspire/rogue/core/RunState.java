package com.inkspire.rogue.core;

import com.inkspire.rogue.content.CardLibrary;
import com.inkspire.rogue.content.CharacterLibrary;
import com.inkspire.rogue.content.PotionLibrary;
import com.inkspire.rogue.content.RelicLibrary;
import com.inkspire.rogue.model.CardDef;
import com.inkspire.rogue.model.CardInstance;
import com.inkspire.rogue.model.PotionDef;
import com.inkspire.rogue.model.Rarity;
import com.inkspire.rogue.model.RelicDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** All persistent state of one run. Combat is rebuilt from this state for safety. */
public final class RunState {
    /** 基础药水栏。遗物可以往上加。 */
    public static final int BASE_POTION_SLOTS = 3;

    public long seed;
    public Random rng;
    public String characterId = CharacterLibrary.defaultId();
    public int act;
    public int hp, maxHp, gold;
    public int maxEnergy = 3;
    public ArrayList<CardInstance> deck = new ArrayList<>();
    public ArrayList<String> relics = new ArrayList<>();
    public ArrayList<PotionState> potions = new ArrayList<>();
    public ArrayList<MapNode> map = new ArrayList<>();
    public ArrayList<String> seenEvents = new ArrayList<>();
    public int currentId = -1;
    public int floor = 0;
    public int ascension = 0;
    public int kills, elitesKilled, score;
    public boolean dead, won;
    public String lastNodeSummary = "";

    public static RunState newRun(long seed) {
        return newRun(seed, CharacterLibrary.defaultId());
    }

    public static RunState newRun(long seed, String characterId) {
        CharacterLibrary.CharacterDef c = CharacterLibrary.get(characterId);
        RunState r = new RunState();
        r.seed = seed;
        r.rng = new Random(seed);
        r.characterId = c.id;
        r.act = 0;
        r.maxHp = c.maxHp;
        r.hp = c.maxHp;
        r.gold = c.gold;
        r.deck.addAll(CardLibrary.starterDeck(c.id));
        r.relics.add(c.starterRelic);
        r.map = MapGenerator.generate(0, r.rng);
        return r;
    }

    public MapNode node(int id) {
        for (MapNode n : map) if (n.id == id) return n;
        return null;
    }

    public MapNode currentNode() { return node(currentId); }

    public List<MapNode> availableNodes() {
        ArrayList<MapNode> out = new ArrayList<>();
        if (currentId < 0) {
            for (MapNode n : map) if (n.row == 0) out.add(n);
            return out;
        }
        MapNode cur = node(currentId);
        if (cur == null) return out;
        for (int id : cur.next) {
            MapNode n = node(id);
            if (n != null) out.add(n);
        }
        return out;
    }

    public void enterNode(MapNode n) {
        currentId = n.id;
        n.visited = true;
        floor = n.row + 1;
    }

    public void nextAct() {
        act++;
        if (act > MapGenerator.ACTS - 1) { won = true; return; }
        currentId = -1;
        floor = 0;
        map = MapGenerator.generate(act, rng);
        lastNodeSummary = "踏入" + MapGenerator.actName(act);
    }

    public int goldGain(int base) {
        int pct = 0;
        for (String id : relics) {
            RelicDef d = RelicLibrary.get(id);
            if (d != null) pct += d.goldPercent;
        }
        return base + (int) Math.ceil(base * pct / 100.0);
    }

    public void gainGold(int base) {
        gold += goldGain(base);
    }

    public boolean hasRelic(String id) { return relics.contains(id); }

    public void addRelic(String id) {
        if (id == null || relics.contains(id)) return;
        relics.add(id);
        RelicDef d = RelicLibrary.get(id);
        if (d != null && d.maxHp > 0) {
            maxHp += d.maxHp;
            hp = Math.min(maxHp, hp + d.maxHp);
        }
    }

    /** 药水栏上限：基础 2 + 遗物加成。 */
    public int potionSlots() {
        int slots = BASE_POTION_SLOTS;
        for (String id : relics) {
            RelicDef d = RelicLibrary.get(id);
            if (d != null) slots += d.potionSlots;
        }
        return slots;
    }

    public boolean addPotion(String id) {
        if (id == null) return false;
        if (potions.size() >= potionSlots()) return false;
        potions.add(new PotionState(id));
        return true;
    }

    public String relicName(String id) {
        RelicDef d = RelicLibrary.get(id);
        return d == null ? id : d.name;
    }

    /* ===================== 事件 / 奖励用的安全助手 ===================== */

    public void heal(int amount) {
        if (amount <= 0) return;
        hp = Math.min(maxHp, hp + amount);
    }

    public void loseHp(int amount) {
        if (amount <= 0) return;
        hp = Math.max(0, hp - amount);
    }

    public void addMaxHp(int amount, boolean alsoHeal) {
        maxHp = Math.max(1, maxHp + amount);
        if (alsoHeal && amount > 0) hp = Math.min(maxHp, hp + amount);
        hp = Math.min(hp, maxHp);
    }

    public RelicDef randomRelicDef() { return RelicLibrary.random(rng, relics); }

    public PotionDef randomPotionDef() { return PotionLibrary.random(rng); }

    public List<CardDef> cardPool() { return CardLibrary.poolOf(characterId); }

    public CardDef randomCard(Rarity rarity) {
        return CardLibrary.randomOfRarity(rng, characterId, rarity);
    }

    public CardDef randomAnyCard() {
        return CardLibrary.randomReward(rng, characterId);
    }

    /** 随机升级一张还没升级的牌；没有则返回 null。 */
    public CardInstance upgradeRandomCard() {
        ArrayList<CardInstance> pool = new ArrayList<>();
        for (CardInstance c : deck) if (!c.upgraded) pool.add(c);
        if (pool.isEmpty()) return null;
        CardInstance c = pool.get(rng.nextInt(pool.size()));
        c.upgraded = true;
        return c;
    }

    public CardInstance removeRandomCard() {
        if (deck.size() <= 1) return null;
        return deck.remove(rng.nextInt(deck.size()));
    }

    public CardInstance duplicateRandomCard() {
        if (deck.isEmpty()) return null;
        CardInstance src = deck.get(rng.nextInt(deck.size()));
        CardInstance copy = src.copy();
        deck.add(copy);
        return copy;
    }

    public void addCard(CardDef def, boolean upgraded) {
        if (def == null) return;
        deck.add(new CardInstance(def, upgraded));
    }

    public boolean addRandomPotion() {
        PotionDef p = randomPotionDef();
        return p != null && addPotion(p.id);
    }

    public boolean addRandomRelic() {
        RelicDef r = randomRelicDef();
        if (r == null) return false;
        addRelic(r.id);
        return true;
    }

    /** 塞一张随机的诅咒 / 状态牌进牌组。 */
    public CardDef addRandomCurse() {
        CardDef c = CardLibrary.randomCurse(rng);
        if (c != null) deck.add(new CardInstance(c));
        return c;
    }

    public CardDef addRandomStatusCard() {
        CardDef c = CardLibrary.randomStatusCard(rng);
        if (c != null) deck.add(new CardInstance(c));
        return c;
    }
}
