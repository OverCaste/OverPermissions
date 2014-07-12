package com.overmc.overpermissions;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableCollection;
import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;

/**
 * A simple interface for a source of data for a cached group.
 */
interface GroupDataSource {
    public int getPriority( );

    public Collection<String> getGlobalPermissions( );

    public Collection<TemporaryPermissionEntry> getGlobalTempPermissions( );

    public Map<String, String> getGlobalMetadata( );

    public Collection<String> getPermissions(String worldName);

    public Collection<TemporaryPermissionEntry> getTempPermissions(String worldName);

    public Map<String, String> getMetadata(String worldName);

    public void addGlobalPermission(String permissionNode);

    public void addGlobalPermissions(Iterable<String> permissionNodes);

    public void removeGlobalPermission(String permissionNode);

    public void removeGlobalPermissions(Iterable<String> permissionNodes);

    public void addPermission(String worldName, String permissionNode);

    public void addPermissions(String worldName, Iterable<String> permissionNodes);

    public void removePermission(String worldName, String permissionNode);

    public void addTempPermission(String worldName, String permissionNode, long timeInMillis);

    public void addTempPermissions(String worldName, ImmutableCollection<TemporaryPermissionEntry> permissionNodes);

    public void removeTempPermission(String worldName, String permissionNode);

    public void addGlobalTempPermission(long timeInMillis, String permissionNode);

    public void addGlobalTempPermissions(Collection<TemporaryPermissionEntry> entries);

    public void removeGlobalTempPermission(String permissionNode);

    public void removeGlobalTempPermissions(Collection<TemporaryPermissionEntry> entries);

    public void setGlobalMeta(String key, String value);

    public void removeGlobalMeta(String key);

    public void setGlobalMetaEntries(Collection<MetadataEntry> entries);

    public void setMeta(String worldName, String key, String value);

    public void removeMeta(String worldName, String key);

    public void setMetaEntries(String worldName, Collection<MetadataEntry> entries);

    public void addParent(PermissionGroup parent);

    public void removeParent(PermissionGroup parent);

    public Collection<String> getParents( );

    public Collection<String> getChildren( );

}
