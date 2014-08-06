package com.overmc.overpermissions.internal.databases;

import com.overmc.overpermissions.internal.datasources.*;

public interface DatabaseMultiSourceFactory extends UUIDDataSourceFactory, UserDataSourceFactory, GroupManagerDataSourceFactory, TemporaryPermissionEntityDataSourceFactory {
    public void shutdown( );
}
