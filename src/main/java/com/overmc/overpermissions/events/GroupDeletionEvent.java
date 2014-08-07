package com.overmc.overpermissions.events;

import org.bukkit.event.HandlerList;

public class GroupDeletionEvent extends GroupEvent {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;

    public GroupDeletionEvent(String groupName) {
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
