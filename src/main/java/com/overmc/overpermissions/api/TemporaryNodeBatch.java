package com.overmc.overpermissions.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * A batch of temporary permissions to be set all at once.
 * 
 * To instantiate, use the {@link Builder}.
 */
public final class TemporaryNodeBatch {
    private final ImmutableList<TemporaryPermissionEntry> globalNodes;
    private final ImmutableMultimap<String, TemporaryPermissionEntry> worldNodes;

    private TemporaryNodeBatch(ImmutableList<TemporaryPermissionEntry> globalNodes, ImmutableMultimap<String, TemporaryPermissionEntry> worldNodes) {
        this.globalNodes = globalNodes;
        this.worldNodes = worldNodes;
    }

    public ImmutableList<TemporaryPermissionEntry> getGlobalNodes( ) {
        return globalNodes;
    }

    public ImmutableMultimap<String, TemporaryPermissionEntry> getWorldNodes( ) {
        return worldNodes;
    }

    public Collection<String> getAllNodes( ) {
        HashSet<String> ret = new HashSet<>(globalNodes.size() + worldNodes.size());
        for (TemporaryPermissionEntry e : globalNodes) {
            ret.add(e.getNode());
        }
        for (TemporaryPermissionEntry e : worldNodes.values()) {
            ret.add(e.getNode());
        }
        return ret;
    }
    
    public static Builder builder( ) {
        return new Builder();
    }

    public static final class Builder {
        private Set<TemporaryPermissionEntry> globalNodes = new HashSet<>();
        private Multimap<String, TemporaryPermissionEntry> worldNodes = HashMultimap.create();

        private Builder( ) {
            //No instantiation.
        }
        
        public Builder addNode(String node, String worldName, long time, TimeUnit unit) {
            Preconditions.checkNotNull(node, "The node can't be null!");
            Preconditions.checkNotNull(node, "The world can't be null!");
            Preconditions.checkNotNull(unit, "The time unit can't be null!");
            Preconditions.checkArgument(time > 0, "You can't add a node for 0 or less time.");
            // Preconditions.checkArgument(worldId >= 0, "A valid world id has to be greater or equal to 0.");
            worldNodes.put(worldName.toLowerCase(), new TemporaryPermissionEntry(node, System.currentTimeMillis()+unit.toMillis(time)));
            return this;
        }

        public Builder addGlobalNode(String node, long time, TimeUnit unit) {
            Preconditions.checkNotNull(node, "The node can't be null!");
            Preconditions.checkNotNull(unit, "The time unit can't be null!");
            Preconditions.checkArgument(time > 0, "You can't add a node for 0 or less time.");
            globalNodes.add(new TemporaryPermissionEntry(node, System.currentTimeMillis()+unit.toMillis(time)));
            return this;
        }

        public TemporaryNodeBatch build( ) {
            return new TemporaryNodeBatch(ImmutableList.copyOf(globalNodes), ImmutableMultimap.copyOf(worldNodes));
        }
    }
}
