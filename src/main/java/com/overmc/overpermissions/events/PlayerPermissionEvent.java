package com.overmc.overpermissions.events;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import com.google.common.base.Preconditions;

public abstract class PlayerPermissionEvent extends Event {
	private final String node;
	private final String world;
	private final String playerName;
	private boolean cancelled;

	public PlayerPermissionEvent(String playerName, String worldName, String node) {
		this.node = node;
		this.world = worldName;
		this.playerName = playerName;
		Preconditions.checkNotNull(node);
		Preconditions.checkNotNull(playerName);
	}

	public String getNode( ) {
		return node;
	}

	/**
	 * @return The name of the player who's permissions are being changed. Guaranteed to not return null.
	 */
	public String getPlayerName( ) {
		return playerName;
	}

	/**
	 * @return The world of the player who's permissions are being changed. Null if the world is global.
	 */
	public String getWorldName( ) {
		return world;
	}

	/**
	 * @return true if this permission is being changed across all worlds, false otherwise.
	 */
	public boolean isGlobal( ) {
		return world == null;
	}

	/**
	 * @return null - If the player who's permissions are being changed is offline, otherwise the player matching the permission changed.
	 * 
	 * @see #getPlayerName()
	 */
	public Player getPlayer( ) {
		return Bukkit.getPlayerExact(playerName);
	}

	/**
	 * @return null - If the world selected is global, otherwise the world in which the permission is being changed.
	 * 
	 * @see #getWorldName()
	 */
	public World getWorld( ) {
		return Bukkit.getWorld(world);
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public boolean isCancelled( ) {
		return cancelled;
	}
}
