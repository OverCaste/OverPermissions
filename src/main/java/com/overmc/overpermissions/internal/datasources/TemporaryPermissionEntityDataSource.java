package com.overmc.overpermissions.internal.datasources;

import com.overmc.overpermissions.api.TemporaryNodeBatch;

public interface TemporaryPermissionEntityDataSource {
    public TemporaryNodeBatch getTempPermissions( );
}
