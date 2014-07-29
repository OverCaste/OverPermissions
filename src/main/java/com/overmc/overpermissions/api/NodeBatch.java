package com.overmc.overpermissions.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * A batch of permissions to be set all at once.
 * 
 * To instantiate, use the {@link Builder}.
 */
public final class NodeBatch {
    private final ImmutableList<String> globalNodes;
    private final ImmutableMultimap<String, String> worldNodes;

    private NodeBatch(ImmutableList<String> globalNodes, ImmutableMultimap<String, String> worldNodes) {
        this.globalNodes = globalNodes;
        this.worldNodes = worldNodes;
    }

    public ImmutableList<String> getGlobalNodes( ) {
        return globalNodes;
    }

    public ImmutableMultimap<String, String> getWorldNodes( ) {
        return worldNodes;
    }

    public Collection<String> getAllNodes( ) {
        HashSet<String> ret = new HashSet<>(globalNodes.size() + worldNodes.size());
        ret.addAll(globalNodes);
        ret.addAll(worldNodes.values());
        return ret;
    }

    public static Builder builder( ) {
        return new Builder();
    }

    public static final class Builder {
        private Set<String> globalNodes = new HashSet<>();
        private Multimap<String, String> worldNodes = HashMultimap.create();

        private Builder( ) {
            // Hide constructor
        }

        public Builder addNode(String node, String worldName) { // TODO documentation
            Preconditions.checkNotNull(node, "The node can't be null!");
            Preconditions.checkNotNull(node, "The world can't be null!");
            worldNodes.put(node, worldName.toLowerCase());
            return this;
        }

        public Builder addGlobalNode(String node) {
            Preconditions.checkNotNull(node, "The node can't be null!");
            globalNodes.add(node);
            return this;
        }

        public NodeBatch build( ) {
            return new NodeBatch(ImmutableList.copyOf(globalNodes), ImmutableMultimap.copyOf(worldNodes));
        }
    }
}
