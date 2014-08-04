package com.overmc.overpermissions.internal.localentities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;
import com.overmc.overpermissions.internal.NodeTree;
import com.overmc.overpermissions.internal.datasources.PermissionEntityDataSource;
import com.overmc.overpermissions.internal.util.PermissionUtils;

public abstract class LocalPermissionEntity {
    private final ReadWriteLock nodesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock tempNodesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock permissionsLock = new ReentrantReadWriteLock();
    private final ReadWriteLock metaLock = new ReentrantReadWriteLock();

    private final Set<String> nodes = new HashSet<>();
    private final Map<String, Boolean> permissions;
    private final Map<String, String> meta = new HashMap<String, String>();

    private final Set<String> tempNodes = new HashSet<>();
    private final ConcurrentMap<String, TemporaryPermissionEntry> tempEntries = new ConcurrentHashMap<>();

    private final PermissionEntityDataSource dataSource;

    private final boolean wildcardSupport;
    
    public LocalPermissionEntity(PermissionEntityDataSource dataSource, boolean wildcardSupport) {
        if(wildcardSupport) {
            permissions = new NodeTree<Boolean>();
        } else {
            permissions = new HashMap<String, Boolean>();
        }
        this.wildcardSupport = wildcardSupport;
        this.dataSource = dataSource;
    }

    protected Set<String> getAllNodes( ) {
        return Sets.union(tempNodes, nodes);
    }

    protected Iterable<ReadWriteLock> getAllNodeLocks( ) {
        return Arrays.asList(nodesLock, tempNodesLock);
    }

    protected void recalculatePermission(String node) {
        String baseNode = PermissionUtils.getBaseNode(node);
        permissionsLock.writeLock().lock();
        for (ReadWriteLock l : getAllNodeLocks()) { // TODO this is pretty ugly, just make readonly collections
            l.readLock().lock();
        }
        try {
            permissions.put(baseNode, PermissionUtils.getPermissionValue(node, baseNode, getAllNodes()));
        } finally {
            for (ReadWriteLock l : getAllNodeLocks()) {
                l.readLock().unlock();
            }
            permissionsLock.writeLock().unlock();
        }
    }

    protected void recalculatePermissions(Iterable<String> nodes) {
        permissionsLock.writeLock().lock();
        for (ReadWriteLock l : getAllNodeLocks()) {
            l.readLock().lock();
        }
        try {
            for (String node : nodes) {
                String baseNode = PermissionUtils.getBaseNode(node);
                permissions.put(baseNode, PermissionUtils.getPermissionValue(node, baseNode, getAllNodes()));
            }
        } finally {
            permissionsLock.writeLock().unlock();
            for (ReadWriteLock l : getAllNodeLocks()) {
                l.readLock().unlock();
            }
        }
    }

    protected void recalculatePermissions( ) {
        permissionsLock.writeLock().lock();
        for (ReadWriteLock l : getAllNodeLocks()) {
            l.readLock().lock();
        }
        try {
            for (String node : nodes) {
                String baseNode = PermissionUtils.getBaseNode(node);
                permissions.put(baseNode, PermissionUtils.getPermissionValue(node, baseNode, getAllNodes()));
            }
        } finally {
            permissionsLock.writeLock().unlock();
            for (ReadWriteLock l : getAllNodeLocks()) {
                l.readLock().unlock();
            }
        }
    }

    public void reloadPermissions( ) {
        nodesLock.writeLock().lock();
        tempNodesLock.writeLock().lock();
        try {
            nodes.clear();
            tempNodes.clear();
            for (String node : dataSource.getPermissions()) {
                nodes.add(node.toLowerCase());
            }
            for (TemporaryPermissionEntry e : dataSource.getTempPermissions()) {
                tempNodes.add(e.getNode().toLowerCase());
            }
        } finally {
            nodesLock.writeLock().unlock();
            tempNodesLock.writeLock().unlock();
        }
    }

    public void reloadMetadata( ) {
        metaLock.writeLock().lock();
        try {
            meta.clear();
            meta.putAll(dataSource.getMetadata());
        } finally {
            metaLock.writeLock().unlock();
        }
    }

    protected boolean hasInternalPermission(String permission) {
        permissionsLock.readLock().lock();
        try {
            return permissions.containsKey(permission.toLowerCase());
        } finally {
            permissionsLock.readLock().unlock();
        }
    }

    protected boolean getInternalPermission(String permission) {
        permissionsLock.readLock().lock();
        try {
            return permissions.get(permission.toLowerCase());
        } finally {
            permissionsLock.readLock().unlock();
        }
    }

    protected boolean addInternalPermissionNode(String permissionNode) {
        nodesLock.writeLock().lock();
        boolean success;
        try {
            success = nodes.add(permissionNode.toLowerCase());
        } finally {
            nodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.addPermission(permissionNode);
            recalculatePermission(permissionNode);
        }
        return success;
    }

    protected boolean addInternalPermissionNodes(Collection<String> permissions) {
        nodesLock.writeLock().lock();
        boolean success = false;
        try {
            for (String node : permissions) {
                if (nodes.add(node.toLowerCase())) {
                    success = true;
                }
            }
        } finally {
            nodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.addPermissions(permissions);
            recalculatePermissions(permissions);
        }
        return success;
    }

    protected boolean removeInternalPermissionNode(String permission) {
        nodesLock.writeLock().lock();
        boolean success;
        try {
            success = nodes.remove(permission.toLowerCase());
        } finally {
            nodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.removePermission(permission);
            recalculatePermission(permission);
        }
        return success;
    }

    protected boolean removeInternalPermissionNodes(Collection<String> permissions) {
        nodesLock.writeLock().lock();
        boolean success = false;
        try {
            for (String node : permissions) {
                if (nodes.remove(node.toLowerCase())) {
                    success = true;
                }
            }
        } finally {
            nodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.removePermissions(permissions);
            recalculatePermissions(permissions);
        }
        return success;
    }

    protected boolean hasInternalPermissionNode(String permissionNode) {
        nodesLock.readLock().lock();
        try {
            return nodes.contains(permissionNode.toLowerCase());
        } finally {
            nodesLock.readLock().unlock();
        }
    }

    protected Collection<String> getInternalPermissionNodes( ) {
        nodesLock.readLock().lock();
        try {
            return new ArrayList<>(nodes);
        } finally {
            nodesLock.readLock().unlock();
        }
    }

    protected Map<String, Boolean> getInternalPermissionValues( ) {
        permissionsLock.readLock().lock();
        try {
            return Maps.newHashMap(permissions);
        } finally {
            permissionsLock.readLock().unlock();
        }
    }

    protected boolean hasInternalTempPermissionNode(String permissionNode) {
        permissionNode = permissionNode.toLowerCase();
        tempNodesLock.readLock().lock();
        try {
            return tempNodes.contains(permissionNode);
        } finally {
            tempNodesLock.readLock().unlock();
        }
    }

    protected boolean addInternalTempPermissionNode(String permission, long time, TimeUnit unit) {
        long timeInMillis = unit.toMillis(time);
        tempNodesLock.writeLock().lock();
        boolean success;
        try {
            success = tempNodes.add(permission.toLowerCase());
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        tempEntries.put(permission, new TemporaryPermissionEntry(permission, System.currentTimeMillis() + timeInMillis));
        if (success) {
            dataSource.addTempPermission(permission, timeInMillis);
            registerTempPermission(permission, timeInMillis);
            recalculatePermission(permission);
        }
        return success;
    }

    protected boolean addInternalTempPermissionNodes(Collection<TemporaryPermissionEntry> permissions) {
        ArrayList<TemporaryPermissionEntry> changedEntries = new ArrayList<>(permissions.size());
        tempNodesLock.writeLock().lock();
        boolean success = false;
        try {
            for (TemporaryPermissionEntry entry : permissions) {
                if (tempNodes.add(entry.getNode().toLowerCase())) {
                    success = true;
                    changedEntries.add(entry);
                }
            }
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.addTempPermissions(permissions);
            recalculatePermissions(Iterables.transform(changedEntries, new Function<TemporaryPermissionEntry, String>() {
                @Override
                public String apply(TemporaryPermissionEntry entry) {
                    return entry.getNode().toLowerCase();
                }
            }));
            for (TemporaryPermissionEntry e : changedEntries) {
                registerTempPermission(e.getNode(), e.getExpirationTime());
            }
        }
        for (TemporaryPermissionEntry entry : permissions) {
            tempEntries.put(entry.getNode().toLowerCase(), entry);
        }
        return success;
    }

    protected boolean removeInternalTempPermissionNode(String permission) {
        permission = permission.toLowerCase();
        tempNodesLock.writeLock().lock();
        boolean success;
        try {
            success = tempNodes.remove(permission);
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.removeTempPermission(permission);
            cancelTempPermission(permission);
            recalculatePermission(permission);
        }
        tempEntries.remove(permission);
        return success;
    }

    protected boolean removeInternalTempPermissionNodes(Collection<TemporaryPermissionEntry> permissions) {
        ArrayList<TemporaryPermissionEntry> changedNodes = new ArrayList<>(permissions.size());
        tempNodesLock.writeLock().lock();
        boolean success = false;
        try {
            for (TemporaryPermissionEntry entry : permissions) {
                if (tempNodes.remove(entry.getNode().toLowerCase())) {
                    success = true;
                    changedNodes.add(entry);
                }
            }
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.removeTempPermissions(permissions);
            recalculatePermissions(Iterables.transform(changedNodes, new Function<TemporaryPermissionEntry, String>() {
                @Override
                public String apply(TemporaryPermissionEntry entry) {
                    return entry.getNode().toLowerCase();
                }
            }));
            for (TemporaryPermissionEntry e : changedNodes) {
                cancelTempPermission(e.getNode());
            }
        }
        for (TemporaryPermissionEntry e : permissions) {
            tempEntries.remove(e.getNode().toLowerCase());
        }
        return success;
    }

    protected Collection<String> getInternalTempPermissionNodes( ) {
        tempNodesLock.readLock().lock();
        try {
            return new ArrayList<>(tempNodes); // Defensive copy
        } finally {
            tempNodesLock.readLock().unlock();
        }
    }

    protected Collection<TemporaryPermissionEntry> getInternalTempPermissionEntries( ) {
        return tempEntries.values();
    }

    protected String getInternalMeta(String key) {
        metaLock.readLock().lock();
        try {
            return meta.get(key);
        } finally {
            metaLock.readLock().unlock();
        }
    }

    protected boolean hasInternalMeta(String key) {
        metaLock.readLock().lock();
        try {
            return meta.containsKey(key);
        } finally {
            metaLock.readLock().unlock();
        }
    }

    protected void setInternalMeta(String key, String value) {
        metaLock.writeLock().lock();
        try {
            meta.put(key, value);
        } finally {
            metaLock.writeLock().unlock();
        }
        dataSource.setMeta(key, value);
    }

    protected void setInternalMetaEntries(Iterable<MetadataEntry> entries) {
        metaLock.writeLock().lock();
        try {
            for (MetadataEntry e : entries) {
                meta.put(e.getKey().toLowerCase(), e.getValue());
            }
        } finally {
            metaLock.writeLock().unlock();
        }
        dataSource.setMetaEntries(entries);
    }

    protected boolean removeInternalMeta(String key) {
        boolean success;
        metaLock.writeLock().lock();
        try {
            success = meta.remove(key) != null;
        } finally {
            metaLock.writeLock().unlock();
        }
        if (success) {
            dataSource.removeMeta(key);
        }
        return success;
    }

    protected void addInternalMetaEntries(Collection<MetadataEntry> entries) {
        ArrayList<MetadataEntry> changedMeta = new ArrayList<>(entries.size());
        metaLock.writeLock().lock();
        try {
            for (MetadataEntry e : entries) {
                if (e.getValue() == null) {
                    if (meta.remove(e.getKey().toLowerCase()) != null) {
                        changedMeta.add(e);
                    }
                } else {
                    meta.put(e.getKey().toLowerCase(), e.getValue());
                    changedMeta.add(e);
                }
            }
        } finally {
            metaLock.writeLock().unlock();
        }
        dataSource.setMetaEntries(changedMeta);
    }

    protected Collection<MetadataEntry> getInternalMetadataEntries( ) {
        ArrayList<MetadataEntry> ret = new ArrayList<>(meta.size());
        for (Map.Entry<String, String> e : meta.entrySet()) {
            ret.add(new MetadataEntry(e.getKey(), e.getValue()));
        }
        return ret;
    }
    
    public boolean areWildcardsSupported( ) {
        return wildcardSupport;
    }

    protected abstract void registerTempPermission(String node, long timeInMillis);

    protected abstract void cancelTempPermission(String node);
}
