package com.inkspire.rogue.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.inkspire.rogue.content.CardLibrary;
import com.inkspire.rogue.model.CardDef;
import com.inkspire.rogue.model.CardInstance;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

public final class SaveStore {
    private static final String PREFS = "ink_spire_save";
    private static final String KEY_RUN = "run";
    private static final String KEY_HINTS = "hints";

    private SaveStore() {}

    public static boolean has(Context ctx) {
        return prefs(ctx).contains(KEY_RUN);
    }

    public static void clear(Context ctx) { prefs(ctx).edit().remove(KEY_RUN).apply(); }

    public static void save(Context ctx, RunState r) {
        if (r == null || r.dead || r.won) { clear(ctx); return; }
        try {
            JSONObject o = new JSONObject();
            o.put("v", 2);
            o.put("char", r.characterId);
            o.put("seed", r.seed);
            o.put("act", r.act);
            o.put("hp", r.hp);
            o.put("maxHp", r.maxHp);
            o.put("gold", r.gold);
            o.put("maxEnergy", r.maxEnergy);
            o.put("currentId", r.currentId);
            o.put("floor", r.floor);
            o.put("ascension", r.ascension);
            o.put("kills", r.kills);
            o.put("elites", r.elitesKilled);
            o.put("score", r.score);
            o.put("summary", r.lastNodeSummary == null ? "" : r.lastNodeSummary);
            JSONArray deck = new JSONArray();
            for (CardInstance c : r.deck) {
                JSONObject d = new JSONObject();
                d.put("id", c.def.id);
                d.put("up", c.upgraded);
                deck.put(d);
            }
            o.put("deck", deck);
            JSONArray relics = new JSONArray();
            for (String s : r.relics) relics.put(s);
            o.put("relics", relics);
            JSONArray potions = new JSONArray();
            for (PotionState p : r.potions) potions.put(p.id);
            o.put("potions", potions);
            JSONArray seen = new JSONArray();
            for (String id : r.seenEvents) seen.put(id);
            o.put("seen", seen);
            JSONArray map = new JSONArray();
            for (MapNode n : r.map) {
                JSONObject m = new JSONObject();
                m.put("id", n.id); m.put("row", n.row); m.put("lane", n.lane);
                m.put("type", n.type.name()); m.put("visited", n.visited);
                JSONArray next = new JSONArray();
                for (int id : n.next) next.put(id);
                m.put("next", next);
                map.put(m);
            }
            o.put("map", map);
            prefs(ctx).edit().putString(KEY_RUN, o.toString()).apply();
        } catch (Exception e) {
            // A failed autosave must never crash a run.
        }
    }

    public static RunState load(Context ctx) {
        String raw = prefs(ctx).getString(KEY_RUN, null);
        if (raw == null) return null;
        try {
            JSONObject o = new JSONObject(raw);
            RunState r = new RunState();
            r.seed = o.optLong("seed", System.currentTimeMillis());
            r.rng = new Random(r.seed ^ System.nanoTime());
            r.characterId = o.optString("char", com.inkspire.rogue.content.CharacterLibrary.defaultId());
            r.act = o.optInt("act", 0);
            r.hp = o.optInt("hp", 80);
            r.maxHp = o.optInt("maxHp", 80);
            r.gold = o.optInt("gold", 0);
            r.maxEnergy = o.optInt("maxEnergy", 3);
            r.currentId = o.optInt("currentId", -1);
            r.floor = o.optInt("floor", 0);
            r.ascension = o.optInt("ascension", 0);
            r.kills = o.optInt("kills", 0);
            r.elitesKilled = o.optInt("elites", 0);
            r.score = o.optInt("score", 0);
            r.lastNodeSummary = o.optString("summary", "");
            JSONArray deck = o.optJSONArray("deck");
            if (deck != null) {
                for (int i = 0; i < deck.length(); i++) {
                    JSONObject d = deck.getJSONObject(i);
                    CardDef def = CardLibrary.get(d.optString("id"));
                    if (def != null) r.deck.add(new CardInstance(def, d.optBoolean("up", false)));
                }
            }
            JSONArray relics = o.optJSONArray("relics");
            if (relics != null) for (int i = 0; i < relics.length(); i++) r.relics.add(relics.getString(i));
            JSONArray potions = o.optJSONArray("potions");
            if (potions != null) for (int i = 0; i < potions.length(); i++) r.potions.add(new PotionState(potions.getString(i)));
            JSONArray seen = o.optJSONArray("seen");
            if (seen != null) for (int i = 0; i < seen.length(); i++) r.seenEvents.add(seen.getString(i));
            JSONArray map = o.optJSONArray("map");
            if (map != null) {
                for (int i = 0; i < map.length(); i++) {
                    JSONObject m = map.getJSONObject(i);
                    NodeType type = NodeType.valueOf(m.optString("type", "BATTLE"));
                    MapNode n = new MapNode(m.optInt("id"), m.optInt("row"), m.optInt("lane"), type);
                    n.visited = m.optBoolean("visited", false);
                    r.map.add(n);
                }
                for (int i = 0; i < map.length(); i++) {
                    JSONObject m = map.getJSONObject(i);
                    MapNode n = r.node(m.optInt("id"));
                    JSONArray next = m.optJSONArray("next");
                    if (n != null && next != null) for (int j = 0; j < next.length(); j++) n.next.add(next.getInt(j));
                }
            }
            if (r.deck.isEmpty() || r.map.isEmpty()) return null;
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hints(Context ctx) { return prefs(ctx).getBoolean(KEY_HINTS, true); }
    public static void setHints(Context ctx, boolean on) { prefs(ctx).edit().putBoolean(KEY_HINTS, on).apply(); }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
