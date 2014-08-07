package com.overmc.overpermissions.internal.datasources;

import java.util.Collection;
import java.util.Map;

import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;

public interface PermissionEntityDataSource {
    public Collection<String> getPermissions( );

    public Collection<TemporaryPermissionEntry> getTempPermissions( );

    public Map<String, String> getMetadata( );

    public void addPermission(String permission);

    public void addPermissions(Iterable<String> permissions);

    public void removePermission(String permission);

    public void removePermissions(Iterable<String> permissions);

    public void addTempPermission(String permission, long timeInMillis);

    public void addTempPermissions(Iterable<TemporaryPermissionEntry> permissions);

    public void removeTempPermission(String permission);

    public void removeTempPermissions(Iterable<TemporaryPermissionEntry> permissions);

    public void setMeta(String key, String value);

    public void removeMeta(String key);

    public void setMetaEntries(Iterable<MetadataEntry> entries);
}
