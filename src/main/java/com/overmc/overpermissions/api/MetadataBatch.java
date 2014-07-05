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
    private final ImmutableList<MetadataEntry> globalNodes;
    private final ImmutableMultimap<String, MetadataEntry> worldNodes;

    private MetadataBatch(ImmutableList<MetadataEntry> globalNodes, ImmutableMultimap<String, MetadataEntry> worldNodes) {
        this.globalNodes = globalNodes;
        this.worldNodes = worldNodes;
    }

    public ImmutableList<MetadataEntry> getGlobalNodes( ) {
        return globalNodes;
    }

    public ImmutableMultimap<String, MetadataEntry> getWorldNodes( ) {
        return worldNodes;
    }

    public Collection<String> getAllKeys( ) {
        HashSet<String> ret = new HashSet<String>(globalNodes.size() + worldNodes.size());
        for (MetadataEntry e : globalNodes) {
            ret.add(e.getKey());
        }
        for (MetadataEntry e : worldNodes.values()) {
            ret.add(e.getKey());
        }
        return ret;
    }

    public Collection<String> getAllValues( ) {
        HashSet<String> ret = new HashSet<String>(globalNodes.size() + worldNodes.size());
        for (MetadataEntry e : globalNodes) {
            if (e.getValue() != null) {
                ret.add(e.getValue());
            }
        }
        for (MetadataEntry e : worldNodes.values()) {
            if (e.getValue() != null) {
                ret.add(e.getValue());
            }
        }
        return ret;
    }

    public static final class Builder {
        private Set<MetadataEntry> globalNodes = new HashSet<>();
        private Multimap<String, MetadataEntry> worldNodes = HashMultimap.create();

        public Builder addGlobalEntry(String key, String value) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            Preconditions.checkNotNull(value, "The metadata value can't be null!");
            globalNodes.add(new MetadataEntry(key, value));
            return this;
        }

        public Builder addEntry(String key, String value, String world) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            Preconditions.checkNotNull(value, "The metadata value can't be null!");
            Preconditions.checkNotNull(world, "The world value can't be null!");
            worldNodes.put(world, new MetadataEntry(key, value));
            return this;
        }

        public Builder addGlobalDeletionEntry(String key) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            globalNodes.add(new MetadataEntry(key, null));
            return this;
        }

        public Builder addDeletionEntry(String key, String world) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            Preconditions.checkNotNull(world, "The world value can't be null!");
            worldNodes.put(world, new MetadataEntry(key, null));
            return this;
        }

        public MetadataBatch build( ) {
            return new MetadataBatch(ImmutableList.copyOf(globalNodes), ImmutableMultimap.copyOf(worldNodes));
        }
    }

}
