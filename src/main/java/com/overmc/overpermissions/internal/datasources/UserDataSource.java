package com.overmc.overpermissions.internal.datasources;

import java.util.Collection;

import com.overmc.overpermissions.api.PermissionGroup;

public interface UserDataSource extends PermissionEntityDataSource, WorldDataSourceFactory {
    public void addParent(PermissionGroup parent);

    public void removeParent(PermissionGroup parent);

    public void setParent(PermissionGroup parent);

    public Collection<String> getParents( );
    
    public boolean doesUserExist( );
}
