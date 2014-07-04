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
 * A batch of key-value pairs of metadata to be set all at once.
 * 
 * To instantiate, use the {@link Builder}.
 */
public final class MetadataBatch {
    private final ImmutableList<Entry> globalNodes;
    private final ImmutableMultimap<String, Entry> worldNodes;

    private MetadataBatch(ImmutableList<Entry> globalNodes, ImmutableMultimap<String, Entry> worldNodes) {
        this.globalNodes = globalNodes;
        this.worldNodes = worldNodes;
    }

    public ImmutableList<Entry> getGlobalNodes( ) {
        return globalNodes;
    }

    public ImmutableMultimap<String, Entry> getWorldNodes( ) {
        return worldNodes;
    }

    public Collection<String> getAllKeys( ) {
        HashSet<String> ret = new HashSet<String>(globalNodes.size() + worldNodes.size());
        for (Entry e : globalNodes) {
            ret.add(e.getKey());
        }
        for (Entry e : worldNodes.values()) {
            ret.add(e.getKey());
        }
        return ret;
    }

    public Collection<String> getAllValues( ) {
        HashSet<String> ret = new HashSet<String>(globalNodes.size() + worldNodes.size());
        for (Entry e : globalNodes) {
            if (e.getValue() != null) {
                ret.add(e.getValue());
            }
        }
        for (Entry e : worldNodes.values()) {
            if (e.getValue() != null) {
                ret.add(e.getValue());
            }
        }
        return ret;
    }

    public static final class Builder {
        private Set<Entry> globalNodes = new HashSet<>();
        private Multimap<String, Entry> worldNodes = HashMultimap.create();

        public Builder addGlobalEntry(String key, String value) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            Preconditions.checkNotNull(value, "The metadata value can't be null!");
            globalNodes.add(new Entry(key, value));
            return this;
        }

        public Builder addEntry(String key, String value, String world) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            Preconditions.checkNotNull(value, "The metadata value can't be null!");
            Preconditions.checkNotNull(world, "The world value can't be null!");
            worldNodes.put(world, new Entry(key, value));
            return this;
        }

        public Builder addGlobalDeletionEntry(String key) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            globalNodes.add(new Entry(key, null));
            return this;
        }

        public Builder addDeletionEntry(String key, String world) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            Preconditions.checkNotNull(world, "The world value can't be null!");
            worldNodes.put(world, new Entry(key, null));
            return this;
        }

        public MetadataBatch build( ) {
            return new MetadataBatch(ImmutableList.copyOf(globalNodes), ImmutableMultimap.copyOf(worldNodes));
        }
    }

    public static final class Entry {
        private final String key;
        private final String value;

        private Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey( ) {
            return key;
        }

        public String getValue( ) {
            return value;
        }
    }
}
