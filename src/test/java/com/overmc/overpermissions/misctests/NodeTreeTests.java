package com.overmc.overpermissions.misctests;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.overmc.overpermissions.internal.wildcardsupport.NodeTree;

public class NodeTreeTests {
    @Test
    public void testNodeExistance( ) {
        Set<String> nodes = new HashSet<String>();
        nodes.addAll(Arrays.asList(new String[] {
                "my.first.node",
                "my.second.node",
                "my.third.node",
                "my.first.node.a",
                "my.first.node.b",
                "my.first.node.b.c",
                "my.third.node.*",
                "my.first.*"
        }));
        NodeTree<Object> tree = new NodeTree<>();
        for (String node : nodes) {
            tree.put(node, null);
        }
        int nodesLeft = nodes.size();
        for (String node : tree.keySet()) {
            if (nodes.contains(node)) {
                nodesLeft--;
            }
        }
        assertEquals("Node count must be the same on insertion and iteration.", nodesLeft, 0);
        assertTrue("The tree must contain my.first.[anynode]", tree.containsKey("my.first.test"));
        assertTrue("The tree must contain my.second.node", tree.containsKey("my.second.node"));
        assertFalse("The tree must not contain not.a.node", tree.containsKey("not.a.node"));
    }

    @Test(expected = NullPointerException.class)
    public void testInsertionNPE( ) {
        (new NodeTree<Object>()).put(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertionIAEOne( ) {
        (new NodeTree<Object>()).put("my..node", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertionIAETwo( ) {
        (new NodeTree<Object>()).put("my.node.", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInsertionEmptyKeyIAE( ) {
        (new NodeTree<Object>()).put("", null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetNPE( ) {
        (new NodeTree<Object>()).get(null);
    }
}
