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
    private final ImmutableList<Entry> globalNodes;
    private final ImmutableMultimap<String, Entry> worldNodes;

    private TemporaryNodeBatch(ImmutableList<Entry> globalNodes, ImmutableMultimap<String, Entry> worldNodes) {
        this.globalNodes = globalNodes;
        this.worldNodes = worldNodes;
    }

    public ImmutableList<Entry> getGlobalNodes( ) {
        return globalNodes;
    }

    public ImmutableMultimap<String, Entry> getWorldNodes( ) {
        return worldNodes;
    }

    public Collection<String> getAllNodes( ) {
        HashSet<String> ret = new HashSet<String>(globalNodes.size() + worldNodes.size());
        for (Entry e : globalNodes) {
            ret.add(e.getNode());
        }
        for (Entry e : worldNodes.values()) {
            ret.add(e.getNode());
        }
        return ret;
    }

    public static final class Builder {
        private Set<Entry> globalNodes = new HashSet<>();
        private Multimap<String, Entry> worldNodes = HashMultimap.create();

        public Builder addNode(String node, String worldName, long time, TimeUnit unit) {
            Preconditions.checkNotNull(node, "The node can't be null!");
            Preconditions.checkNotNull(node, "The world can't be null!");
            Preconditions.checkNotNull(unit, "The time unit can't be null!");
            Preconditions.checkArgument(time > 0, "You can't add a node for 0 or less time.");
            // Preconditions.checkArgument(worldId >= 0, "A valid world id has to be greater or equal to 0.");
            worldNodes.put(worldName, new Entry(node, unit.toMillis(time)));
            return this;
        }

        public Builder addGlobalNode(String node, long time, TimeUnit unit) {
            Preconditions.checkNotNull(node, "The node can't be null!");
            Preconditions.checkNotNull(unit, "The time unit can't be null!");
            Preconditions.checkArgument(time > 0, "You can't add a node for 0 or less time.");
            globalNodes.add(new Entry(node, unit.toMillis(time)));
            return this;
        }

        public TemporaryNodeBatch build( ) {
            return new TemporaryNodeBatch(ImmutableList.copyOf(globalNodes), ImmutableMultimap.copyOf(worldNodes));
        }
    }

    public static final class Entry {
        private final String node;
        private final long timeInMillis;

        private Entry(String node, long timeInMillis) {
            this.node = node;
            this.timeInMillis = timeInMillis;
        }

        public String getNode( ) {
            return node;
        }

        public long getTimeInMillis( ) {
            return timeInMillis;
        }
    }
}
