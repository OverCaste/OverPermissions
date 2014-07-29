package com.overmc.overpermissions.internal.datasources;

import java.util.Collection;

import com.overmc.overpermissions.api.PermissionGroup;

/**
 * A simple interface for a source of data for a cached group.
 */
public interface GroupDataSource extends PermissionEntityDataSource, WorldDataSourceFactory {
    public int getPriority( );

    public void addParent(PermissionGroup parent);

    public void removeParent(PermissionGroup parent);

    public Collection<String> getParents( );

    public Collection<String> getChildren( );
}
