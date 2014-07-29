package com.overmc.overpermissions.internal.localentities;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.common.collect.Sets;
import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.PermissionEntityDataSource;

public class LocalUserWorldData extends LocalTransientPermissionEntity {
    private final LocalUser user;
    private final String worldName;
    private final TemporaryPermissionManager tempManager;

    private final Set<String> allParentNodes = new CopyOnWriteArraySet<>();
    private final Map<String, String> parentMetadataMap = new ConcurrentHashMap<>();

    public LocalUserWorldData(LocalUser user, String worldName, TemporaryPermissionManager tempManager, PermissionEntityDataSource source) {
        super(source);
        this.user = user;
        this.worldName = worldName;
        this.tempManager = tempManager;
    }

    @Override
    protected Set<String> getAllNodes( ) {
        return Sets.union(super.getAllNodes(), allParentNodes);
    }

    @Override
    protected void registerTempPermission(String node, long timeInMillis) {
        tempManager.registerWorldTemporaryPermission(user, worldName, new TemporaryPermissionEntry(node, System.currentTimeMillis() + timeInMillis));
    }

    @Override
    protected void cancelTempPermission(String node) {
        tempManager.cancelWorldTemporaryPermission(user, worldName, node);
    }

    public boolean hasGroupMeta(String key) {
        return parentMetadataMap.containsKey(key.toLowerCase());
    }

    public String getGroupMeta(String key) {
        return parentMetadataMap.get(key.toLowerCase());
    }

    public void addGroupNodes(Collection<String> nodes) {
        allParentNodes.addAll(nodes);
    }

    public void clearGroupNodes( ) {
        allParentNodes.clear();
    }

    public void addGroupMeta(Collection<MetadataEntry> metaEntries) {
        for (MetadataEntry e : metaEntries) {
            parentMetadataMap.put(e.getKey(), e.getValue());
        }
    }

    public void clearGroupMeta( ) {
        parentMetadataMap.clear();
    }
}
