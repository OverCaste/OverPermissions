package com.overmc.overpermissions.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class NodeTree<E> implements Map<String, E> {
    private static final Splitter splitter = Splitter.on('.');

    private final Map<String, NodeTree<E>> subNodeContainers = new HashMap<>(4); // Nodes that have this node as a subparent. (this.subnode.subsubnode)
    private final Map<String, E> physicalSubNodes = new HashMap<>(4); // Nodes that have this node as their only parent. (this.subnode)

    private int size = 0;

    @Override
    public E put(String node, E value) {
        Preconditions.checkNotNull(node, "node");
        node = node.toLowerCase();
        List<String> split = Lists.newArrayList(splitter.split(node));
        for (String s : split) {
            if (s == null || s.length() == 0) {
                throw new IllegalArgumentException("String \"" + node + "\" can't be parsed into a NodeTree.");
            }
        }
        return setInternal(split, value);
    }

    private E setInternal(List<String> subnodes, E value) {
        if (subnodes.isEmpty()) {
            return null;
        }
        String nodeName = subnodes.get(0);
        if (subnodes.size() == 1) {
            E ret = physicalSubNodes.put(nodeName, value);
            if (ret == null) { // A new element was added
                size++;
            }
            return ret;
        } else {
            NodeTree<E> subNode;
            if (subNodeContainers.containsKey(nodeName)) {
                subNode = subNodeContainers.get(nodeName);
            } else {
                subNode = new NodeTree<>();
                subNodeContainers.put(nodeName, subNode);
            }
            E ret = subNode.setInternal(subnodes.subList(1, subnodes.size()), value);
            if (ret == null) { // A new element was added
                size++;
            }
            return ret;
        }
    }

    @Override
    public E remove(Object key) {
        Preconditions.checkNotNull(key, "key");
        if (!(key instanceof String)) {
            return null;
        }
        String node = (String) key;
        List<String> split = Arrays.asList(node.split("\\."));
        return removeInternal(split);
    }

    private E removeInternal(List<String> subnodes) {
        if (subnodes.isEmpty()) {
            return null;
        }
        String nodeName = subnodes.get(0);
        if (subnodes.size() == 1) {
            E ret = physicalSubNodes.remove(nodeName);
            if (ret != null) { // An element was removed
                size--;
            }
            return ret;
        } else {
            if (subNodeContainers.containsKey(nodeName)) {
                NodeTree<E> subNode = subNodeContainers.get(nodeName);
                E ret = subNode.removeInternal(subnodes.subList(1, subnodes.size()));
                if (ret != null) { // A new element was added
                    size--;
                }
                return ret;
            } else {
                return null;
            }
        }
    }

    @Override
    public boolean containsKey(Object key) {
        Preconditions.checkNotNull(key, "key");
        if (!(key instanceof String)) {
            return false;
        }
        String node = (String) key;
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

    @Override
    public E get(Object key) {
        Preconditions.checkNotNull(key, "key");
        if (!(key instanceof String)) {
            return null;
        }
        String node = (String) key;
        List<String> split = Arrays.asList(node.toLowerCase().split("\\."));
        return getInternal(split);
    }

    private E getInternal(List<String> subnodes) {
        E value = null;
        String baseNodeName = subnodes.get(0);
        if (subnodes.size() == 1) { // Try to match full nodes before wildcard nodes.
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
        if (physicalSubNodes.containsKey("*")) { // node.* would match node.subnode.*
            return physicalSubNodes.get("*");
        }
        return value;
    }

    @Override
    public void clear( ) {
        subNodeContainers.clear();
        physicalSubNodes.clear();
    }

    @Override
    public boolean containsValue(Object object) {
        if (physicalSubNodes.containsValue(object)) {
            return true;
        }
        for (NodeTree<E> subTree : subNodeContainers.values()) {
            if (subTree.containsValue(object)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Map.Entry<String, E>> entrySet( ) {
        throw new UnsupportedOperationException("Can't get the entry set of a Node Tree."); // TODO
    }

    @Override
    public boolean isEmpty( ) {
        return subNodeContainers.isEmpty() && physicalSubNodes.isEmpty();
    }

    @Override
    public Set<String> keySet( ) { // TODO back by collection
        List<StringBuilder> builders = new ArrayList<>();
        enumerateSubnodes(builders, new StringBuilder());
        Set<String> ret = new HashSet<String>(builders.size());
        for (StringBuilder b : builders) {
            ret.add(b.toString());
        }
        return ret;
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
    public Collection<E> values( ) {
        throw new UnsupportedOperationException("Can't get the values of a Node Tree.");
    }

    @Override
    public void putAll(Map<? extends String, ? extends E> otherMap) {
        throw new UnsupportedOperationException("Can't putAll into a Node Tree.");
    }

    @Override
    public int size( ) {
        return size;
    }

    @Override
    public String toString( ) {
        return "NodeTree [" + subNodeContainers + ", " + physicalSubNodes + "]";
    }
}