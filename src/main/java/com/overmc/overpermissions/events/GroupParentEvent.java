package com.overmc.overpermissions.events;

import com.google.common.base.Preconditions;

public abstract class GroupParentEvent extends GroupEvent {
    private final String parentName;
    private boolean cancelled;

    public GroupParentEvent(String groupName, String parentName) {
        super(groupName);
        Preconditions.checkNotNull(parentName);
        this.parentName = parentName;
    }

    /**
     * @return the name of the parent which is being changed.
     */
    public String getParentName( ) {
        return parentName;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled( ) {
        return cancelled;
    }
}
