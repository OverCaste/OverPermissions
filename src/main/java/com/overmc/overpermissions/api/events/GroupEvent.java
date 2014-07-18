package com.overmc.overpermissions.api.events;

import org.bukkit.event.Event;

import com.google.common.base.Preconditions;

public abstract class GroupEvent extends Event {
    private final String groupName;

    public GroupEvent(String groupName) {
        Preconditions.checkNotNull(groupName);
        this.groupName = groupName;
    }

    /**
     * @return the name of the group being affected by this event.
     */
    public String getGroupName( ) {
        return groupName;
    }
}
