package com.overmc.overpermissions.api.events;

import org.bukkit.event.HandlerList;

public class PlayerMetaRemoveEvent extends PlayerMetaEvent {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList( ) {
        return handlers;
    }

    public PlayerMetaRemoveEvent(String playerName, String worldName, String key) {
        super(playerName, worldName, key);
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }
}
