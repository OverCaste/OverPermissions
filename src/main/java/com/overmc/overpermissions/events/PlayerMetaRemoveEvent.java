package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.overmc.overpermissions.PermissionChangeCause;

public class PlayerMetaRemoveEvent extends PlayerMetaEvent {

    private static final HandlerList handlers = new HandlerList();
    private final PermissionChangeCause cause;

    public static HandlerList getHandlerList( ) {
        return handlers;
    }

    public PlayerMetaRemoveEvent(Player who, String meta, PermissionChangeCause cause) {
        super(who, meta);
        this.cause = cause;
    }

    public PermissionChangeCause getCause( ) {
        return cause;
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }

}
