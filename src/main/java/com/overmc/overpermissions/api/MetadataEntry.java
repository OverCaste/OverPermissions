package com.overmc.overpermissions.api;

import java.util.Objects;

import com.google.common.base.Preconditions;

public final class MetadataEntry {
    private final String key;
    private final String value;

    public MetadataEntry(String key, String value) {
        Preconditions.checkNotNull(key, "Key can't be null!");
        this.key = key;
        this.value = value;
    }

    public String getKey( ) {
        return key;
    }

    public String getValue( ) {
        return value;
    }

    @Override
    public int hashCode( ) {
        return Objects.hash(key, value);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof MetadataEntry)) {
            return false;
        }
        return (Objects.equals(((MetadataEntry) other).key, key) && Objects.equals(((MetadataEntry) other).value, value));
    }

    @Override
    public String toString( ) {
        return "Metadata entry " + key + ": " + value;
    }
}
