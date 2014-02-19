package com.overmc.overpermissions.events;

import org.bukkit.event.HandlerList;

import com.overmc.overpermissions.PermissionChangeCause;

public class PlayerPermissionAddEvent extends PlayerPermissionEvent {

	private static final HandlerList handlers = new HandlerList();
	private final PermissionChangeCause cause;

	public static HandlerList getHandlerList( ) {
		return handlers;
	}

	public PlayerPermissionAddEvent(String playerName, String worldName, String node, PermissionChangeCause cause) {
		super(playerName, worldName, node);
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
