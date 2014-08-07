package com.overmc.overpermissions.internal.datasources;

import java.util.UUID;

public interface TemporaryPermissionEntityDataSourceFactory {
    public TemporaryPermissionEntityDataSource createTempGroupDataSource(String groupName);

    public TemporaryPermissionEntityDataSource createTempPlayerDataSource(UUID playerUniqueId);
}
