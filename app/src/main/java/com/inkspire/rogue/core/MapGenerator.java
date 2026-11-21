package com.inkspire.rogue.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 三列十二行、四幕。单节点行是地标，让路线读得清、跑得快。 */
public final class MapGenerator {
    /** 幕数（0..ACTS-1）。 */
    public static final int ACTS = 4;
    public static final int ROWS = 12;

    public static ArrayList<MapNode> generate(int act, Random rng) {
        ArrayList<MapNode> nodes = new ArrayList<>();
        ArrayList<ArrayList<MapNode>> rows = new ArrayList<>();
        for (int row = 0; row < ROWS; row++) {
            int count = countFor(row, rng);
            List<Integer> lanes = pickLanes(count, rng);
            ArrayList<MapNode> rowNodes = new ArrayList<>();
            for (int lane : lanes) {
                NodeType type = typeFor(act, row, rng);
                MapNode n = new MapNode(row * 10 + lane, row, lane, type);
                rowNodes.add(n);
                nodes.add(n);
            }
            // 精英行只放一个精英，其余是普通战斗：玩家永远可以绕开，不会被硬塞。
            if (row == 5 || row == 8) {
                for (MapNode n : rowNodes) n.type = NodeType.BATTLE;
                rowNodes.get(rng.nextInt(rowNodes.size())).type = NodeType.ELITE;
            }
            rows.add(rowNodes);
        }
        for (int row = 0; row < ROWS - 1; row++) {
            ArrayList<MapNode> from = rows.get(row);
            ArrayList<MapNode> to = rows.get(row + 1);
            for (MapNode a : from) {
                int links = 0;
                for (MapNode b : to) {
                    if (Math.abs(a.lane - b.lane) <= 1) {
                        a.next.add(b.id);
                        links++;
                    }
                }
                if (links == 0) a.next.add(nearest(a.lane, to).id);
            }
            // Guarantee every destination can be reached from this row.
            for (MapNode b : to) {
                boolean any = false;
                for (MapNode a : from) if (a.next.contains(b.id)) { any = true; break; }
                if (!any) nearest(b.lane, from).next.add(b.id);
            }
        }
        return nodes;
    }

    private static int countFor(int row, Random rng) {
        if (row == 0) return 3;
        if (row == 3 || row == 4 || row == 7 || row == 10 || row == 11) return 1;
        if (row == 1 || row == 4 || row == 7) return rng.nextBoolean() ? 2 : 3;
        if (row == 5 || row == 8) return rng.nextBoolean() ? 2 : 3;
        return rng.nextInt(3) == 0 ? 2 : 3;
    }

    private static List<Integer> pickLanes(int count, Random rng) {
        ArrayList<Integer> lanes = new ArrayList<>();
        if (count <= 1) { lanes.add(1); return lanes; }
        if (count == 2) {
            if (rng.nextBoolean()) { lanes.add(0); lanes.add(1); }
            else { lanes.add(1); lanes.add(2); }
            return lanes;
        }
        lanes.add(0); lanes.add(1); lanes.add(2);
        return lanes;
    }

    private static MapNode nearest(int lane, List<MapNode> list) {
        MapNode best = list.get(0);
        for (MapNode n : list) if (Math.abs(n.lane - lane) < Math.abs(best.lane - lane)) best = n;
        return best;
    }

    private static NodeType typeFor(int act, int row, Random rng) {
        switch (row) {
            case 0: return NodeType.BATTLE;
            case 1: return rng.nextInt(3) == 0 ? NodeType.EVENT : NodeType.BATTLE;
            case 2: return rng.nextBoolean() ? NodeType.EVENT : NodeType.BATTLE;
            case 3: return NodeType.TREASURE;
            case 4: return NodeType.REST;
            case 5: return NodeType.BATTLE;
            case 6: return rng.nextBoolean() ? NodeType.BATTLE : NodeType.EVENT;
            case 7: return NodeType.SHOP;
            case 8: return NodeType.BATTLE;
            case 9: return rng.nextBoolean() ? NodeType.BATTLE : NodeType.EVENT;
            case 10: return NodeType.REST;
            case 11: return NodeType.BOSS;
            default: return NodeType.BATTLE;
        }
    }

    public static String actName(int act) {
        switch (act) {
            case 0: return "第一幕 · 腐化回廊";
            case 1: return "第二幕 · 无声书库";
            case 2: return "第三幕 · 塔尖";
            default: return "第四幕 · 落款";
        }
    }

    public static String actSubtitle(int act) {
        switch (act) {
            case 0: return "书脊之下，墨迹仍在呼吸。";
            case 1: return "所有句子都已学会沉默。";
            case 2: return "翻到最后一页，塔便醒来。";
            default: return "签上名字的人，从此住在页里。";
        }
    }
}
