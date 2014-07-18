package com.overmc.overpermissions.api.events;

import org.bukkit.event.HandlerList;

public class GroupPermissionRemoveEvent extends GroupPermissionEvent {
    private static final HandlerList handlers = new HandlerList();

    private final boolean temporary;
    
    public static HandlerList getHandlerList( ) {
        return handlers;
    }
    
    public GroupPermissionRemoveEvent(String groupName, String node, String worldName) {
        this(groupName, node, worldName, false);
    }

    public GroupPermissionRemoveEvent(String groupName, String node, String worldName, boolean temporary) {
        super(groupName, node, worldName);
        this.temporary = temporary;
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }

    public boolean isTemporary( ) {
        return temporary;
    }
}
