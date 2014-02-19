package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

public abstract class PlayerMetaEvent extends PlayerEvent {
	private final String node;
	private boolean cancelled;

	public PlayerMetaEvent(Player who, String node) {
		super(who);
		this.node = node;
	}

	public String getNode( ) {
		return node;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public boolean isCancelled( ) {
		return cancelled;
	}
}
