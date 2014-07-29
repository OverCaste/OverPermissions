package com.overmc.overpermissions.internal.datasources;

public interface GroupDataSourceFactory {
    public GroupDataSource createGroupDataSource(String groupName);
}
