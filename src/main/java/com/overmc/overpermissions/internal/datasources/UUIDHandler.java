package com.overmc.overpermissions.internal.datasources;

import java.util.UUID;

import com.overmc.overpermissions.exceptions.InvalidOnlineUsernameException;

public interface UUIDHandler {
    public UUID getNameUuid(String name);

    public UUID getOrCreateNameUuid(String name) throws InvalidOnlineUsernameException;

    public void setNameUuid(String name, UUID uuid);
    
    public String getLastSeenName(UUID uuid);
}
