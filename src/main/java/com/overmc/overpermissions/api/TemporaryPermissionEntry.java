package com.overmc.overpermissions.api;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class TemporaryPermissionEntry {
    private final String node;
    private final long timeoutInMillis;

    public TemporaryPermissionEntry(String node, long timeoutInMillis) {
        Preconditions.checkNotNull(node, "The node can't be null!");
        Preconditions.checkArgument(timeoutInMillis > 0, "Timeout must be greater than 0.");
        this.node = node;
        this.timeoutInMillis = timeoutInMillis;
    }

    public String getNode( ) {
        return node;
    }

    /**
     * @return the time in milliseconds that this entry will expire at.
     */
    public long getExpirationTime( ) {
        return timeoutInMillis;
    }

    @Override
    public int hashCode( ) {
        return Objects.hash(node, Long.valueOf(timeoutInMillis));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof TemporaryPermissionEntry)) {
            return false;
        }
        return ((TemporaryPermissionEntry) other).timeoutInMillis == timeoutInMillis && Objects.equals(((TemporaryPermissionEntry) other).node, node);
    }

    @Override
    public String toString( ) {
        return "TemporaryPermissionEntry " + node + " for " + timeoutInMillis + "ms.";
    }
}
