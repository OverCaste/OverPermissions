package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.overmc.overpermissions.PermissionChangeCause;

public class PlayerMetaAddEvent extends PlayerMetaEvent {

    private static final HandlerList handlers = new HandlerList();
    private final PermissionChangeCause cause;
    private final String value;

    public static HandlerList getHandlerList( ) {
        return handlers;
    }

    public PlayerMetaAddEvent(Player who, String meta, String value, PermissionChangeCause cause) {
        super(who, meta);
        this.cause = cause;
        this.value = value;
    }

    public PermissionChangeCause getCause( ) {
        return cause;
    }

    public String getValue( ) {
        return value;
    }

    @Override
    public HandlerList getHandlers( ) {
        return handlers;
    }

}
