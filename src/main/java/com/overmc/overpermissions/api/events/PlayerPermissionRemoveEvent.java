package com.overmc.overpermissions.api.events;

import org.bukkit.event.HandlerList;

public class PlayerPermissionRemoveEvent extends PlayerPermissionEvent {
    private static final HandlerList handlers = new HandlerList();

    private final boolean temporary;
    
    public static HandlerList getHandlerList( ) {
        return handlers;
    }

    public PlayerPermissionRemoveEvent(String playerName, String worldName, String node, boolean temporary) {
        super(playerName, worldName, node);
        this.temporary = temporary;
    }
    
    public PlayerPermissionRemoveEvent(String playerName, String worldName, String node) {
        this(playerName, worldName, node, false);
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }

    public boolean isTemporary( ) {
        return temporary;
    }
}
