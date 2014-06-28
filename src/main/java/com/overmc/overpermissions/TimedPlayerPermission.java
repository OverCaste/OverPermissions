package com.overmc.overpermissions;

import java.util.Objects;

public class TimedPlayerPermission {
    public final int worldId;
    public final int playerId;
    public final String node;
    public final long executeTime;

    public TimedPlayerPermission(int worldId, int playerId, String permission, long timeout) {
        this.worldId = worldId;
        this.playerId = playerId;
        this.node = permission;
        this.executeTime = timeout;
    }

    @Override
    public int hashCode( ) {
        return Objects.hash(worldId, playerId, node);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TimedPlayerPermission) {
            TimedPlayerPermission otherTimed = (TimedPlayerPermission) other;
            return ((otherTimed.worldId == worldId) && (otherTimed.playerId == playerId) && Objects.equals(node, otherTimed.node));
        }
        return false;
    }
}
