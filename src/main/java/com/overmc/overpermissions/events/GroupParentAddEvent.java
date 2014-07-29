package com.overmc.overpermissions.events;

import org.bukkit.event.HandlerList;

public class GroupParentAddEvent extends GroupParentEvent {
    private static final HandlerList handlers = new HandlerList();

    public GroupParentAddEvent(String groupName, String parentName) {
        super(groupName, parentName);
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }
}
