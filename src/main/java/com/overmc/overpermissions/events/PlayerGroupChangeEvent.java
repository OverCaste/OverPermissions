package com.overmc.overpermissions.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public abstract class PlayerGroupChangeEvent extends Event {
    private final String playerName;
    private final String groupName;

    private boolean cancelled = false;

    public PlayerGroupChangeEvent(String playerName, String group) {
        this.playerName = playerName;
        this.groupName = group;
    }

    /**
     * @return The player who's group changed,
     *         null if they aren't online.
     */
    @SuppressWarnings("deprecation")
    public Player getPlayer( ) {
        return Bukkit.getPlayerExact(this.playerName);
    }

    public String getPlayerName( ) {
        return this.playerName;
    }

    public String getGroupName( ) {
        return this.groupName;
    }

    public boolean isCancelled( ) {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
