package com.overmc.overpermissions.uuid;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.OverPermissions;

public class UUIDManager {
    private final OverPermissions plugin;

    public UUIDManager(OverPermissions plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempts to retrieve a player's permissions UID, or create it in SQL if it exists
     * 
     * @param name
     * @return The UID if it's retrievable, or -1 if the player's UUID wasn't retrievable.
     */
    public int getOrCreateSqlUser(String name) {
        Player p = Bukkit.getPlayerExact(name);
        int playerId = (p == null) ? plugin.getSQLManager().getPlayerId(name) : plugin.getPlayerPermissions(p).getId();
        if (playerId >= 0) {
            return playerId;
        }
        // Doesn't exist in SQL and isn't online
        try {
            UUID playerUuid = UUIDFetcher.getUUIDOf(name);
            return plugin.getSQLManager().getPlayerId(playerUuid, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return -1; // Failed to retrieve even that
    }
}
