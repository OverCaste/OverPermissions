package com.overmc.overpermissions.api;

import java.util.HashMap;
import java.util.Iterator;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * A batch of permissions to be set all at once.
 * 
 * To instantiate, use the {@link Builder}.
 */
public final class NodeBatch implements Iterable<NodeBatch.Entry> {
    private final ImmutableList<Entry> entries;

    private NodeBatch(ImmutableList<Entry> entries) {
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

        public Builder addNode(String node, String worldName) { // TODO documentation
            Preconditions.checkNotNull(node, "The node can't be null!");
            Preconditions.checkNotNull(node, "The world can't be null!");
            // Preconditions.checkArgument(worldName >= 0, "A valid world id has to be greater or equal to 0.");
            entries.put(node, new Entry(node, worldName));
            return this;
        }

        public Builder addGlobalNode(String node) {
            Preconditions.checkNotNull(node, "The node can't be null!");
            entries.put(node, new Entry(node, null));
            return this;
        }

        public NodeBatch build( ) {
            return new NodeBatch(ImmutableList.copyOf(entries.values()));
        }
    }

    public static final class Entry {
        private final String node;
        private final String worldName;

        private Entry(String node, String worldName) {
            this.node = node;
            this.worldName = worldName;
        }

        public String getNode( ) {
            return node;
        }

        public String getWorldName( ) {
            return worldName;
        }

        public boolean isGlobal( ) {
            return worldName == null;
        }
    }
}
