package com.overmc.overpermissions.events;

import org.bukkit.event.HandlerList;

public class GroupMetaClearEvent extends GroupMetaEvent {
    private static final HandlerList handlers = new HandlerList();

    public GroupMetaClearEvent(String playerName, String worldName, String key) {
        super(playerName, worldName, key);
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }
}
