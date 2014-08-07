package com.overmc.nodedisplayapi;

import java.util.*;

public class ElementBoxNode {
    private final String name;
    private final List<ElementBoxNode> subNodes;
    private final List<String> lines;

    public ElementBoxNode(String name, List<ElementBoxNode> subNodes, List<String> lines) {
        if (subNodes == null) {
            this.subNodes = Collections.emptyList();
        } else {
            this.subNodes = new ArrayList<>(subNodes);
        }
        if (lines == null) {
            this.lines = Collections.emptyList();
        } else {
            this.lines = new ArrayList<>(lines);
        }
        this.name = name;
    }

    public String getName( ) {
        return name;
    }

    public List<ElementBoxNode> getSubNodes( ) {
        return Collections.unmodifiableList(subNodes);
    }

    public List<String> getLines( ) {
        return Collections.unmodifiableList(lines);
    }

    public int size( ) {
        int size = 0;
        if (name != null) {
            size += 1;
        }
        size += lines.size();
        for (ElementBoxNode node : subNodes) {
            size += node.size();
        }
        return size;
    }
}
