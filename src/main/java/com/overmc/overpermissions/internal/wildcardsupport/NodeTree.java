package com.overmc.overpermissions.internal.wildcardsupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

public class NodeTree<E> implements Iterable<String> {
    private final Map<String, NodeTree<E>> subNodeContainers = new HashMap<>(4); // Nodes that have this node as a subparent. (this.subnode.subsubnode)
    private final Map<String, E> physicalSubNodes = new HashMap<>(4); // Nodes that have this node as their only parent. (this.subnode)

    public void set(String node, E value) {
        Preconditions.checkNotNull(node, "node");
        node = node.toLowerCase();
        List<String> split = Arrays.asList(node.split("\\."));
        setInternal(split, value);
    }

    private void setInternal(List<String> subnodes, E value) {
        if (subnodes.isEmpty()) {
            return;
        }
        String nodeName = subnodes.get(0);
        if (subnodes.size() == 1) {
            if (physicalSubNodes.put(nodeName, value) == null) {
                // subNodeCount++;
            }
        } else {
            NodeTree<E> subNode;
            if (subNodeContainers.containsKey(nodeName)) {
                subNode = subNodeContainers.get(nodeName);
            } else {
                subNode = new NodeTree<>();
                subNodeContainers.put(nodeName, subNode);
                // subNodeCount++;
            }
            subNode.setInternal(subnodes.subList(1, subnodes.size()), value);
        }
    }

    public boolean has(String node) {
        List<String> split = Arrays.asList(node.toLowerCase().split("\\."));
        return hasInternal(split);
    }

    private boolean hasInternal(List<String> subnodes) {
        String baseNodeName = subnodes.get(0);
        if (physicalSubNodes.containsKey("*")) { // node.* would match node.subnode.*
            return true;
        }
        if (subnodes.size() == 1) {
            return physicalSubNodes.containsKey(baseNodeName);
        } else {
            if (subNodeContainers.containsKey(baseNodeName)) {
                if (subNodeContainers.get(baseNodeName).hasInternal(subnodes.subList(1, subnodes.size()))) {
                    return true;
                }
            }
            if (subNodeContainers.containsKey("*")) {
                if (subNodeContainers.get("*").hasInternal(subnodes.subList(1, subnodes.size()))) {
                    return true;
                }
            }
        }
        return false;
    }

    public E get(String node) {
        List<String> split = Arrays.asList(node.toLowerCase().split("\\."));
        return getInternal(split);
    }

    private E getInternal(List<String> subnodes) {
        E value = null;
        String baseNodeName = subnodes.get(0);
        if (physicalSubNodes.containsKey("*")) { // node.* would match node.subnode.*
            return physicalSubNodes.get("*");
        }
        if (subnodes.size() == 1) {
            if (physicalSubNodes.containsKey(baseNodeName)) {
                return physicalSubNodes.get(baseNodeName);
            }
        } else {
            if (subNodeContainers.containsKey(baseNodeName)) {
                if ((value = subNodeContainers.get(baseNodeName).getInternal(subnodes.subList(1, subnodes.size()))) != null) {
                    return value;
                }
            }
            if (subNodeContainers.containsKey("*")) {
                if (((value = subNodeContainers.get("*").getInternal(subnodes.subList(1, subnodes.size())))) != null) {
                    return value;
                }
            }
        }
        return value;
    }

    public void clear( ) {
        subNodeContainers.clear();
        physicalSubNodes.clear();
    }

    private void enumerateSubnodes(List<StringBuilder> list, StringBuilder base) {
        for (Map.Entry<String, NodeTree<E>> entry : subNodeContainers.entrySet()) {
            StringBuilder subBase = new StringBuilder(base);
            if (base.length() != 0) {
                subBase.append(".");
            }
            entry.getValue().enumerateSubnodes(list, subBase.append(entry.getKey()));
        }
        for (String node : physicalSubNodes.keySet()) {
            list.add(new StringBuilder(base).append(".").append(node));
        }
    }

    @Override
    public Iterator<String> iterator( ) {
        ArrayList<StringBuilder> subnodeBuilders = new ArrayList<>();
        enumerateSubnodes(subnodeBuilders, new StringBuilder());
        ArrayList<String> nodeList = new ArrayList<>(subnodeBuilders.size());
        for (StringBuilder b : subnodeBuilders) {
            nodeList.add(b.toString());
        }
        return nodeList.iterator();
    }

    @Override
    public String toString( ) {
        return "NodeTree [" + Joiner.on(",").join(subNodeContainers.entrySet()) + "]";
    }
}