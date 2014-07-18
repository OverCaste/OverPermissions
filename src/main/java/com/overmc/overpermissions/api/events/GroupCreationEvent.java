package com.overmc.overpermissions.api.events;

import org.bukkit.event.HandlerList;

public class GroupCreationEvent extends GroupEvent {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;

    public GroupCreationEvent(String groupName) {
        super(groupName);
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }

    public boolean isCancelled( ) {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
