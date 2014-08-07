package com.overmc.overpermissions.internal.datasources;

public interface WorldDataSourceFactory {
    public PermissionEntityDataSource createWorldDataSource(String worldName);
}
