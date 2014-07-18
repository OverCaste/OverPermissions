package com.overmc.overpermissions.api.events;

import org.bukkit.event.HandlerList;

public class PlayerMetaAddEvent extends PlayerMetaEvent {
    private static final HandlerList handlers = new HandlerList();
    private final String value;

    public static HandlerList getHandlerList( ) {
        return handlers;
    }

    public PlayerMetaAddEvent(String playerName, String worldName, String key, String value) {
        super(playerName, worldName, key);
        this.value = value;
    }

    public String getValue( ) {
        return value;
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }
}
