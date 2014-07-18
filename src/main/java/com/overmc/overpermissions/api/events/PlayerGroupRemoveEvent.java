package com.overmc.overpermissions.api.events;

import org.bukkit.event.HandlerList;

public class PlayerGroupRemoveEvent extends PlayerGroupChangeEvent {
    private static final HandlerList handlers = new HandlerList();

    public PlayerGroupRemoveEvent(String playerName, String group) {
        super(playerName, group);
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }
}
