package com.overmc.overpermissions.api;

import java.util.HashMap;
import java.util.Iterator;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * A batch of key-value pairs of metadata to be set all at once.
 * 
 * To instantiate, use the {@link Builder}.
 */
public final class MetadataBatch implements Iterable<MetadataBatch.Entry> {
    private final ImmutableList<Entry> entries;

    private MetadataBatch(ImmutableList<Entry> entries) {
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

        public Builder addGlobalEntry(String key, String value) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            Preconditions.checkNotNull(value, "The metadata value can't be null!");
            entries.put(key, new Entry(key, value, null));
            return this;
        }

        public Builder addEntry(String key, String value, String world) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            Preconditions.checkNotNull(value, "The metadata value can't be null!");
            Preconditions.checkNotNull(world, "The world value can't be null!");
            entries.put(key, new Entry(key, value, world));
            return this;
        }

        public Builder addGlobalDeletionEntry(String key) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            entries.put(key, new Entry(key, null, null));
            return this;
        }

        public Builder addDeletionEntry(String key, String world) {
            Preconditions.checkNotNull(key, "The metadata key can't be null!");
            Preconditions.checkNotNull(world, "The world value can't be null!");
            entries.put(key, new Entry(key, null, world));
            return this;
        }

        public MetadataBatch build( ) {
            return new MetadataBatch(ImmutableList.copyOf(entries.values()));
        }
    }

    public static final class Entry {
        private final String key;
        private final String value;
        private final String world;

        private Entry(String key, String value, String world) {
            this.key = key;
            this.value = value;
            this.world = world;
        }

        public String getKey( ) {
            return key;
        }

        public String getValue( ) {
            return value;
        }

        public String getWorld( ) {
            return world;
        }

        public boolean isGlobal( ) {
            return (world == null);
        }
    }
}
