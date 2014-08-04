package com.overmc.overpermissions.internal.localentities;

import com.overmc.overpermissions.api.TemporaryPermissionEntry;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.PermissionEntityDataSource;

public class LocalUserWorldData extends LocalTransientPermissionEntity {
    private final LocalUser user;
    private final String worldName;
    private final TemporaryPermissionManager tempManager;

    public LocalUserWorldData(LocalUser user, String worldName, TemporaryPermissionManager tempManager, PermissionEntityDataSource source) {
        super(source, user.areWildcardsSupported());
        this.user = user;
        this.worldName = worldName;
        this.tempManager = tempManager;
    }

    @Override
    protected void registerTempPermission(String node, long timeInMillis) {
        tempManager.registerWorldTemporaryPermission(user, worldName, new TemporaryPermissionEntry(node, System.currentTimeMillis() + timeInMillis));
    }

    @Override
    protected void cancelTempPermission(String node) {
        tempManager.cancelWorldTemporaryPermission(user, worldName, node);
    }
}
