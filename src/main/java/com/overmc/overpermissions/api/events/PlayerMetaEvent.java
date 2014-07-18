package com.overmc.overpermissions.api.events;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import com.google.common.base.Preconditions;

public abstract class PlayerMetaEvent extends Event {
    private final String key;
    private final String world;
    private final String playerName;
    private boolean cancelled;

    public PlayerMetaEvent(String playerName, String worldName, String key) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(playerName);
        this.key = key;
        this.world = worldName;
        this.playerName = playerName;
    }

    public String getKey( ) {
        return key;
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
        return Bukkit.getWorld(world);
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled( ) {
        return cancelled;
    }
}
