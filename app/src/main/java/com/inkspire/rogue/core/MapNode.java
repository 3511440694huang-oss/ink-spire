package com.inkspire.rogue.core;

import java.util.ArrayList;
import java.util.List;

public final class MapNode {
    public final int id;
    public final int row;
    public final int lane;
    public NodeType type;
    public final List<Integer> next = new ArrayList<>();
    public boolean visited;

    public MapNode(int id, int row, int lane, NodeType type) {
        this.id = id; this.row = row; this.lane = lane; this.type = type;
    }
}
