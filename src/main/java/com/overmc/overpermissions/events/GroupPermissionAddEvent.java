package com.overmc.overpermissions.events;

import org.bukkit.event.HandlerList;

public class GroupPermissionAddEvent extends GroupPermissionEvent {
    private static final HandlerList handlers = new HandlerList();

    private final boolean temporary;

    public static HandlerList getHandlerList( ) {
        return handlers;
    }

    public GroupPermissionAddEvent(String groupName, String node, String worldName, boolean temporary) {
        super(groupName, node, worldName);
        this.temporary = temporary;
    }

    public GroupPermissionAddEvent(String groupName, String node, String worldName) {
        this(groupName, node, worldName, false);
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }

    public boolean isTemporary( ) {
        return temporary;
    }
}
