package com.overmc.overpermissions.internal.databases;

import java.io.IOException;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.base.Charsets;
import com.overmc.overpermissions.exceptions.InvalidOnlineUsernameException;
import com.overmc.overpermissions.internal.datasources.UUIDHandler;
import com.overmc.overpermissions.internal.uuidutils.UUIDFetcher;

// The operations that are most expensive are in order:
// 1. Getting an online UUID from Mojang's servers
// 2. Retrieving a UUID from the database
// 3. Creating an offline UUID/Retrieving a player's UUID
public abstract class AbstractUUIDDataSource implements UUIDHandler {
    @Override
    public UUID getNameUuid(String name) {
        @SuppressWarnings("deprecation")
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) {
            return p.getUniqueId();
        }
        return getDatabaseNameUuid(name);
    }

    public abstract UUID getDatabaseNameUuid(String name);

    @Override
    public UUID getOrCreateNameUuid(String name) throws InvalidOnlineUsernameException {
        @SuppressWarnings("deprecation")
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) {
            return p.getUniqueId();
        }
        if (!Bukkit.getOnlineMode()) {
            return getOfflineUuid(name);
        }
        UUID uuid = getDatabaseNameUuid(name);
        if (uuid != null) {
            return uuid;
        }
        uuid = getOnlineUuid(name);
        setNameUuid(name, uuid);
        return uuid;
    }

    protected UUID getOfflineUuid(String playerName) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8));
    }

    protected UUID getOnlineUuid(String playerName) throws InvalidOnlineUsernameException {
        UUIDFetcher f = new UUIDFetcher(Arrays.asList(playerName));
        try {
            Map<String, UUID> value = f.call();
            if (value.containsKey(playerName)) {
                return value.get(playerName);
            } else {
                throw new InvalidOnlineUsernameException("There wasn't a mojang account associated with the name \"" + playerName + ".\"");
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to retrieve online UUID from Mojang's servers.", e);
        }
    }
}
