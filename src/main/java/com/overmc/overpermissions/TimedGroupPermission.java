package com.overmc.overpermissions;

import java.util.Objects;

public class TimedGroupPermission {
    public final int groupId;
    public final String node;
    public final long executeTime;

    public TimedGroupPermission(int groupId, String permission, long timeout) {
        this.groupId = groupId;
        node = permission;
        executeTime = timeout;
    }

    @Override
    public int hashCode( ) {
        return Objects.hash(groupId, node);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof TimedGroupPermission) {
            TimedGroupPermission otherTimed = (TimedGroupPermission) other;
            return ((otherTimed.groupId == groupId) && Objects.equals(node, otherTimed.node));
        }
        return false;
    }
}
