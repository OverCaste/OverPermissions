package com.overmc.overpermissions.api.events;

import org.bukkit.event.HandlerList;

public class GroupRemoveParentEvent extends GroupEvent {
    private static final HandlerList handlers = new HandlerList();

    private final String parentName;

    private boolean cancelled;

    public GroupRemoveParentEvent(String groupName, String parentName) {
        super(groupName);
        this.parentName = parentName;
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }

    public String getParentName( ) {
        return parentName;
    }

    public boolean isCancelled( ) {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
