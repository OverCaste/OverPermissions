package com.overmc.overpermissions.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerGroupChangeEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final String playerName;
	private final String group;
	private final int groupPriority;
	private final EventType type;

	private boolean enabled = false;

	public PlayerGroupChangeEvent(String playerName, String group, int groupPriority, EventType type) {
		this.playerName = playerName;
		this.group = group;
		this.groupPriority = groupPriority;
		this.type = type;
	}

	/**
	 * @return The player who's group changed,
	 *         null if they aren't online.
	 */
	public Player getPlayer( ) {
		return Bukkit.getPlayerExact(this.playerName);
	}

	public String getPlayerName( ) {
		return this.playerName;
	}

	public String getGroup( ) {
		return this.group;
	}

	public int getGroupPriority( ) {
		return this.groupPriority;
	}

	public EventType getType( ) {
		return this.type;
	}

	public boolean isEnabled( ) {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public HandlerList getHandlers( ) {
		return handlers;
	}

	public static HandlerList getStaticHandlers( ) {
		return handlers;
	}

	public static enum EventType {
		SET, ADD, REMOVE
	}
}
