package com.overmc.overpermissions.internal.datasources;

import java.util.UUID;

public interface UserDataSourceFactory {
    public UserDataSource createUserDataSource(UUID userUuid);
}
