package com.overmc.overpermissions.internal.localentities;

import com.overmc.overpermissions.api.TemporaryPermissionEntry;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.PermissionEntityDataSource;

public class LocalGroupWorldData extends LocalPermissionEntity {
    private final LocalGroup group;
    private final TemporaryPermissionManager tempManager;

    private final String worldName;

    public LocalGroupWorldData(LocalGroup group, String worldName, TemporaryPermissionManager tempManager, PermissionEntityDataSource dataSource) {
        super(dataSource);
        this.group = group;
        this.worldName = worldName;
        this.tempManager = tempManager;
    }

    @Override
    protected void registerTempPermission(String node, long timeInMillis) {
        tempManager.registerWorldTemporaryPermission(group, worldName, new TemporaryPermissionEntry(node, System.currentTimeMillis() + timeInMillis));
    }

    @Override
    protected void cancelTempPermission(String node) {
        tempManager.cancelWorldTemporaryPermission(group, worldName, node);
    }
}
