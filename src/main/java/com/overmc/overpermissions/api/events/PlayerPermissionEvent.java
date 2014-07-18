package com.overmc.overpermissions.api.events;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import com.google.common.base.Preconditions;

public abstract class PlayerPermissionEvent extends Event {
    private final String node;
    private final String worldName;
    private final String playerName;
    private boolean cancelled;

    public PlayerPermissionEvent(String playerName, String worldName, String node) {
        Preconditions.checkNotNull(node);
        Preconditions.checkNotNull(playerName);
        this.node = node;
        this.worldName = worldName;
        this.playerName = playerName;
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
        return worldName;
    }

    /**
     * @return true if this permission is being changed across all worlds, false otherwise.
     */
    public boolean isGlobal( ) {
        return worldName == null;
    }

    /**
     * @return null - If the player who's permissions are being changed is offline, otherwise the player matching the permission changed.
     * 
     * @see #getPlayerName()
     */
    @SuppressWarnings("deprecation")
    public Player getPlayer( ) {
        return Bukkit.getPlayerExact(playerName);
    }

    /**
     * @return null - If the world selected is global, otherwise the world in which the permission is being changed.
     * 
     * @see #getWorldName()
     */
    public World getWorld( ) {
        if (worldName == null) {
            return null;
        }
        return Bukkit.getWorld(worldName);
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled( ) {
        return cancelled;
    }
}
