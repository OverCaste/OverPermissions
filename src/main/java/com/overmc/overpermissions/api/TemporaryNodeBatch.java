package com.overmc.overpermissions.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * A batch of temporary permissions to be set all at once.
 * 
 * To instantiate, use the {@link Builder}.
 */
public final class TemporaryNodeBatch implements Iterable<TemporaryNodeBatch.Entry> {
    private final ImmutableList<Entry> entries;

    private TemporaryNodeBatch(ImmutableList<Entry> entries) {
        this.entries = entries;
    }

    public ImmutableList<Entry> getEntries( ) {
        return entries;
    }

    @Override
    public Iterator<Entry> iterator( ) {
        return entries.iterator();
    }

    public static final class Builder {
        private final HashMap<String, Entry> entries = new HashMap<>();

        public Builder addNode(String node, String worldName, long time, TimeUnit unit) {
            Preconditions.checkNotNull(node, "The node can't be null!");
            Preconditions.checkNotNull(node, "The world can't be null!");
            Preconditions.checkNotNull(unit, "The time unit can't be null!");
            Preconditions.checkArgument(time > 0, "You can't add a node for 0 or less time.");
            // Preconditions.checkArgument(worldId >= 0, "A valid world id has to be greater or equal to 0.");
            entries.put(node, new Entry(node, worldName, unit.toMillis(time)));
            return this;
        }

        public Builder addGlobalNode(String node, long time, TimeUnit unit) {
            Preconditions.checkNotNull(node, "The node can't be null!");
            Preconditions.checkNotNull(unit, "The time unit can't be null!");
            Preconditions.checkArgument(time > 0, "You can't add a node for 0 or less time.");
            entries.put(node, new Entry(node, null, unit.toMillis(time)));
            return this;
        }

        public TemporaryNodeBatch build( ) {
            return new TemporaryNodeBatch(ImmutableList.copyOf(entries.values()));
        }
    }

    public static final class Entry {
        private final String node;
        private final String worldName;
        private final long timeInMillis;

        private Entry(String node, String worldName, long timeInMillis) {
            this.node = node;
            this.worldName = worldName;
            this.timeInMillis = timeInMillis;
        }

        public String getNode( ) {
            return node;
        }

        public String getWorldName( ) {
            return worldName;
        }

        public long getTimeInMillis( ) {
            return timeInMillis;
        }

        public boolean isGlobal( ) {
            return (worldName == null);
        }
    }
}
