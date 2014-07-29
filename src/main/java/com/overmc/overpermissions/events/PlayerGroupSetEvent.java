package com.overmc.overpermissions.events;

import org.bukkit.event.HandlerList;

public class PlayerGroupSetEvent extends PlayerGroupChangeEvent {
    private static final HandlerList handlers = new HandlerList();

    public PlayerGroupSetEvent(String playerName, String group) {
        super(playerName, group);
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }
}
