package com.overmc.overpermissions.api.events;

import org.bukkit.event.HandlerList;

public class PlayerPermissionAddEvent extends PlayerPermissionEvent {
    private static final HandlerList handlers = new HandlerList();

    private boolean temporary;

    public PlayerPermissionAddEvent(String playerName, String worldName, String node, boolean temporary) {
        super(playerName, worldName, node);
        this.temporary = temporary;
    }

    public PlayerPermissionAddEvent(String playerName, String worldName, String node) {
        this(playerName, worldName, node, false);
    }

    public boolean isTemporary( ) {
        return temporary;
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }
}
