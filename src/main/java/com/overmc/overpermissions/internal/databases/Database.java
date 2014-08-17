package com.overmc.overpermissions.internal.databases;

import com.overmc.overpermissions.internal.datasources.*;

public interface Database extends UserDataSourceFactory, GroupManagerDataSourceFactory, TemporaryPermissionEntityDataSourceFactory {
    public void shutdown( );
    public UUIDHandler getUUIDHandler( );
}
