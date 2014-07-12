package com.overmc.overpermissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Sets;
import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;

class LocalGroupWorldData {
    private final LocalGroup group;
    private final GroupDataSource dataSource;
    private final TemporaryPermissionManager tempManager;

    private final String worldName;

    private final ReadWriteLock nodesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock permissionsLock = new ReentrantReadWriteLock();
    private final ReadWriteLock metaLock = new ReentrantReadWriteLock();
    private final ReadWriteLock tempNodesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock tempPermissionsLock = new ReentrantReadWriteLock();

    private final HashSet<String> nodes = new HashSet<>();
    private final HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();
    private final HashMap<String, String> meta = new HashMap<String, String>();

    private final Set<String> tempNodes = new HashSet<>();
    private final Map<String, Boolean> tempPermissions = new HashMap<String, Boolean>();

    public LocalGroupWorldData(LocalGroup group, String worldName, TemporaryPermissionManager tempManager, GroupDataSource dataSource) {
        this.group = group;
        this.worldName = worldName;
        this.dataSource = dataSource;
        this.tempManager = tempManager;
    }

    protected Set<String> getAllNodes( ) {
        return Sets.union(tempNodes, nodes);
    }

    protected void recalculatePermission(String node) {
        String baseNode = (node.startsWith("+") || node.startsWith("-")) ? node.substring(1) : node;
        permissionsLock.writeLock().lock();
        nodesLock.readLock().lock();
        try {
            permissions.put(baseNode, PermissionUtils.getPermissionValue(node, baseNode, getAllNodes()));
        } finally {
            permissionsLock.writeLock().unlock();
            nodesLock.readLock().unlock();
        }
    }

    protected void recalculatePermissions(Iterable<String> nodes) {
        permissionsLock.writeLock().lock();
        nodesLock.readLock().lock();
        try {
            for (String node : nodes) {
                String baseNode = (node.startsWith("+") || node.startsWith("-")) ? node.substring(1) : node;
                permissions.put(baseNode, PermissionUtils.getPermissionValue(node, baseNode, getAllNodes()));
            }
        } finally {
            permissionsLock.writeLock().unlock();
            nodesLock.readLock().unlock();
        }
    }

    protected void recalculatePermissions( ) {
        permissionsLock.writeLock().lock();
        nodesLock.readLock().lock();
        try {
            for (String node : nodes) {
                String baseNode = (node.startsWith("+") || node.startsWith("-")) ? node.substring(1) : node;
                permissions.put(baseNode, PermissionUtils.getPermissionValue(node, baseNode, getAllNodes()));
            }
        } finally {
            permissionsLock.writeLock().unlock();
            nodesLock.readLock().unlock();
        }
    }

    protected void recalculateTempPermission(String node) {
        String baseNode = (node.startsWith("+") || node.startsWith("-")) ? node.substring(1) : node;
        tempNodesLock.readLock().lock();
        tempPermissionsLock.writeLock().lock();
        try {
            tempPermissions.put(baseNode, PermissionUtils.getPermissionValue(node, baseNode, tempNodes));
        } finally {
            tempNodesLock.readLock().unlock();
            tempPermissionsLock.writeLock().unlock();
        }
    }

    protected void recalculateTempPermissions(Iterable<TemporaryPermissionEntry> changedNodes) {
        tempNodesLock.readLock().lock();
        tempPermissionsLock.writeLock().lock();
        try {
            for (TemporaryPermissionEntry e : changedNodes) {
                String node = e.getNode().toLowerCase();
                String baseNode = (node.startsWith("+") || node.startsWith("-")) ? node.substring(1) : node;
                tempPermissions.put(baseNode, PermissionUtils.getPermissionValue(node, baseNode, tempNodes));
            }
        } finally {
            tempNodesLock.readLock().unlock();
            tempPermissionsLock.writeLock().unlock();
        }
    }

    protected void recalculateTempPermissions( ) {
        tempNodesLock.readLock().lock();
        tempPermissionsLock.writeLock().lock();
        try {
            for (String node : tempNodes) {
                String baseNode = (node.startsWith("+") || node.startsWith("-")) ? node.substring(1) : node;
                tempPermissions.put(baseNode, PermissionUtils.getPermissionValue(node, baseNode, tempNodes));
            }
        } finally {
            tempNodesLock.readLock().unlock();
            tempPermissionsLock.writeLock().unlock();
        }
    }

    public void reloadPermissions( ) {
        tempManager.cancelTemporaryPermissions(worldName, group);
        nodesLock.writeLock().lock();
        tempNodesLock.writeLock().lock();
        try {
            nodes.clear();
            tempNodes.clear();
            nodes.addAll(dataSource.getPermissions(worldName));
            for (TemporaryPermissionEntry e : dataSource.getTempPermissions(worldName)) {
                tempNodes.add(e.getNode());
                tempManager.registerTemporaryPermission(worldName, e.getTimeInMillis(), e.getNode(), group);
                ;
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
            meta.putAll(dataSource.getMetadata(worldName));
        } finally {
            metaLock.writeLock().unlock();
        }
    }

    public boolean hasPermission(String permission) {
        permissionsLock.readLock().lock();
        try {
            return permissions.containsKey(permission.toLowerCase());
        } finally {
            permissionsLock.readLock().unlock();
        }
    }

    public boolean getPermission(String permission) {
        permissionsLock.readLock().lock();
        try {
            return permissions.get(permission.toLowerCase());
        } finally {
            permissionsLock.readLock().unlock();
        }
    }

    public boolean addPermission(String permission) {
        nodesLock.writeLock().lock();
        boolean success;
        try {
            success = nodes.add(permission.toLowerCase());
        } finally {
            nodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.addPermission(worldName, permission);
            recalculatePermission(permission);
        }
        return success;
    }

    public boolean addPermissions(ImmutableCollection<String> permissions) {
        nodesLock.writeLock().lock();
        boolean success;
        try {
            success = nodes.addAll(permissions);
        } finally {
            nodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.addPermissions(worldName, permissions);
            recalculatePermissions(permissions);
        }
        return success;
    }

    public boolean removePermission(String permission) {
        nodesLock.writeLock().lock();
        boolean success;
        try {
            success = nodes.remove(permission.toLowerCase());
        } finally {
            nodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.removePermission(worldName, permission);
            recalculatePermission(permission);
        }
        return success;
    }

    public Collection<String> getNodes( ) {
        nodesLock.readLock().lock();
        try {
            return new ArrayList<>(nodes);
        } finally {
            nodesLock.readLock().unlock();
        }
    }

    public boolean hasTempPermission(String permission) {
        permission = permission.toLowerCase();
        tempPermissionsLock.readLock().lock();
        try {
            return tempPermissions.containsKey(permission);
        } finally {
            tempPermissionsLock.readLock().unlock();
        }
    }

    public boolean getTemporaryPermission(String permission) {
        permission = permission.toLowerCase();
        tempPermissionsLock.readLock().lock();
        try {
            return tempPermissions.get(permission);
        } finally {
            tempPermissionsLock.readLock().unlock();
        }
    }

    public boolean addTempPermission(String permission, long time, TimeUnit unit) {
        long timeInMillis = unit.toMillis(time);
        tempNodesLock.writeLock().lock();
        boolean success;
        try {
            success = tempNodes.add(permission.toLowerCase());
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.addTempPermission(worldName, permission, timeInMillis);
            tempManager.registerTemporaryPermission(worldName, timeInMillis, permission, group);
            recalculateTempPermission(permission);
        }
        return success;
    }

    public boolean addTempPermissions(ImmutableCollection<TemporaryPermissionEntry> permissions) {
        ArrayList<TemporaryPermissionEntry> changedNodes = new ArrayList<>(permissions.size());
        tempNodesLock.writeLock().lock();
        boolean success = false;
        try {
            for (TemporaryPermissionEntry entry : permissions) {
                if (tempNodes.add(entry.getNode())) {
                    success = true;
                    changedNodes.add(entry);
                }
            }
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.addTempPermissions(worldName, permissions);
            recalculateTempPermissions(changedNodes);
            for (TemporaryPermissionEntry e : changedNodes) {
                tempManager.registerTemporaryPermission(worldName, e.getTimeInMillis(), e.getNode(), group);
            }
        }
        return success;
    }

    public boolean removeTempPermission(String permission) {
        tempNodesLock.writeLock().lock();
        boolean success;
        try {
            success = tempNodes.remove(permission.toLowerCase());
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.removeTempPermission(worldName, permission);
            tempManager.cancelTemporaryPermission(worldName, permission, group);
            recalculateTempPermission(permission);
        }
        return success;
    }

    public boolean removeTempPermissions(ImmutableCollection<TemporaryPermissionEntry> permissions) {
        ArrayList<TemporaryPermissionEntry> changedNodes = new ArrayList<>(permissions.size());
        tempNodesLock.writeLock().lock();
        boolean success = false;
        try {
            for (TemporaryPermissionEntry entry : permissions) {
                if (tempNodes.remove(entry.getNode())) {
                    success = true;
                    changedNodes.add(entry);
                }
            }
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        if (success) {
            dataSource.addTempPermissions(worldName, permissions);
            recalculateTempPermissions(changedNodes);
            for (TemporaryPermissionEntry e : changedNodes) {
                tempManager.cancelTemporaryPermission(worldName, e.getNode(), group);
            }
        }
        return success;
    }

    public Collection<String> getTempNodes( ) {
        tempNodesLock.readLock().lock();
        try {
            return new ArrayList<>(tempNodes); // Defensive copy
        } finally {
            tempNodesLock.readLock().unlock();
        }
    }

    public String getMeta(String key) {
        metaLock.readLock().lock();
        try {
            return meta.get(key);
        } finally {
            metaLock.readLock().unlock();
        }
    }

    public boolean hasMeta(String key) {
        metaLock.readLock().lock();
        try {
            return meta.containsKey(key);
        } finally {
            metaLock.readLock().unlock();
        }
    }

    public void setMeta(String key, String value) {
        metaLock.writeLock().lock();
        try {
            meta.put(key, value);
        } finally {
            metaLock.writeLock().unlock();
        }
        dataSource.setMeta(worldName, key, value);
    }

    public boolean removeMeta(String key) {
        boolean success;
        metaLock.writeLock().lock();
        try {
            success = meta.remove(key) != null;
        } finally {
            metaLock.writeLock().unlock();
        }
        if (success) {
            dataSource.removeMeta(worldName, key);
        }
        return success;
    }

    public void addMetaEntries(ImmutableCollection<MetadataEntry> entries) {
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
        dataSource.setMetaEntries(worldName, changedMeta);
    }

    public Collection<MetadataEntry> getMetadataEntries( ) {
        ArrayList<MetadataEntry> ret = new ArrayList<>(meta.size());
        for (Map.Entry<String, String> e : meta.entrySet()) {
            ret.add(new MetadataEntry(e.getKey(), e.getValue()));
        }
        return ret;
    }
}
