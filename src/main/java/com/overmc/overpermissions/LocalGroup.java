package com.overmc.overpermissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.overmc.overpermissions.api.GroupManager;
import com.overmc.overpermissions.api.MetadataBatch;
import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.NodeBatch;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.TemporaryNodeBatch;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;

/**
 * A class that represents a ram stored version of a database group.
 */
class LocalGroup implements PermissionGroup {
    private final GroupDataSource dataSource;
    private final TemporaryPermissionManager tempManager;

    private final String name;
    private final UUID uniqueId;
    private int priority;
    private final Set<PermissionGroup> parents = new CopyOnWriteArraySet<>(); // These are fast for iteration, but fairly slow for modification.
    private final Set<PermissionGroup> children = new CopyOnWriteArraySet<>();

    // World specific data
    private final ConcurrentMap<String, LocalGroupWorldData> worldDataMap = new ConcurrentHashMap<>();

    // The actual, concrete data
    private final Set<String> nodes = new HashSet<>();
    private final Map<String, Boolean> permissions = new HashMap<String, Boolean>();
    private final Map<String, String> meta = new HashMap<String, String>();

    // Temporary data.
    private final Set<String> tempNodes = new HashSet<>();
    private final Map<String, Boolean> tempPermissions = new HashMap<String, Boolean>();

    // Concurrency locks
    private final ReadWriteLock priorityLock = new ReentrantReadWriteLock();
    private final ReadWriteLock nodesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock permissionsLock = new ReentrantReadWriteLock();
    private final ReadWriteLock tempNodesLock = new ReentrantReadWriteLock();
    private final ReadWriteLock tempPermissionsLock = new ReentrantReadWriteLock();
    private final ReadWriteLock metaLock = new ReentrantReadWriteLock();

    // TODO private final HashSet<PermissionUser> playersInGroup = new HashSet<>();

    public LocalGroup(GroupDataSource groupSource, TemporaryPermissionManager tempManager, String name) {
        this(groupSource, tempManager, name, 0);
    }

    public LocalGroup(GroupDataSource groupSource, TemporaryPermissionManager tempManager, String name, int priority) {
        Preconditions.checkNotNull(groupSource, "groupSource");
        Preconditions.checkNotNull(tempManager, "tempManager");
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkArgument(priority >= 0, "Priority must be greater than or equal to 0.");
        this.dataSource = groupSource;
        this.tempManager = tempManager;
        this.name = name;
        this.uniqueId = UUID.nameUUIDFromBytes(("LocalGroup:" + name).getBytes(Charsets.UTF_8));
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

    protected void recalculateTempPermissions(Iterable<String> nodes) {
        tempNodesLock.readLock().lock();
        tempPermissionsLock.writeLock().lock();
        try {
            for (String node : nodes) {
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
            for (String node : nodes) {
                String baseNode = (node.startsWith("+") || node.startsWith("-")) ? node.substring(1) : node;
                tempPermissions.put(baseNode, PermissionUtils.getPermissionValue(node, baseNode, tempNodes));
            }
        } finally {
            tempNodesLock.readLock().unlock();
            tempPermissionsLock.writeLock().unlock();
        }
    }

    public void reloadPermissions( ) {
        tempManager.cancelGlobalTemporaryPermissions(this);
        nodesLock.writeLock().lock();
        tempNodesLock.writeLock().lock();
        try {
            nodes.clear();
            tempNodes.clear();
            nodes.addAll(dataSource.getGlobalPermissions());
            for (TemporaryPermissionEntry e : dataSource.getGlobalTempPermissions()) {
                tempNodes.add(e.getNode());
                tempManager.registerGlobalTemporaryPermission(e.getTimeInMillis(), e.getNode(), this);
            }
        } finally {
            nodesLock.writeLock().unlock();
            tempNodesLock.writeLock().unlock();
        }
        recalculatePermissions();
        recalculateTempPermissions();
    }

    public void reloadMetadata( ) {
        metaLock.writeLock().lock();
        try {
            meta.clear();
            meta.putAll(dataSource.getGlobalMetadata());
        } finally {
            metaLock.writeLock().unlock();
        }
    }

    public void reloadWorldPermissions( ) {
        for (LocalGroupWorldData world : worldDataMap.values()) {
            world.reloadPermissions();
            world.recalculatePermissions();
            world.recalculateTempPermissions();
        }
    }

    public void reloadWorldMetadata( ) {
        for (LocalGroupWorldData world : worldDataMap.values()) {
            world.reloadMetadata();
        }
    }

    public void reloadParentsAndChildren(GroupManager groupManager) {
        Collection<String> newParentNames = dataSource.getParents();
        Collection<PermissionGroup> newParents = new ArrayList<PermissionGroup>(newParentNames.size());
        for (String name : newParentNames) {
            newParents.add(groupManager.getGroup(name));
        }
        Collection<String> newChildrenNames = dataSource.getChildren();
        Collection<PermissionGroup> newChildren = new ArrayList<PermissionGroup>(newChildrenNames.size());
        for (String name : newChildrenNames) {
            newChildren.add(groupManager.getGroup(name));
        }
        parents.clear();
        parents.addAll(newParents);
        children.clear();
        children.addAll(newChildren);
    }

    private LocalGroupWorldData getOrCreateWorld(String name) {
        name = name.toLowerCase();
        LocalGroupWorldData world = worldDataMap.get(name);
        if (world == null) {
            world = new LocalGroupWorldData(this, name, tempManager, dataSource);
            world.reloadMetadata();
            world.reloadPermissions();
            world.recalculatePermissions();
            world.recalculateTempPermissions();
            worldDataMap.put(name, world);
        }
        return world;
    }

    @Override
    public boolean hasGlobalPermission(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        permissionsLock.readLock().lock();
        try {
            return permissions.containsKey(permission);
        } finally {
            permissionsLock.readLock().unlock();
        }
    }

    @Override
    public boolean hasPermission(String permission, String worldName) {
        Preconditions.checkNotNull(permission, "permission");
        Preconditions.checkNotNull(worldName, "worldName");
        if (worldDataMap.containsKey(worldName.toLowerCase())) {
            LocalGroupWorldData w = worldDataMap.get(worldName);
            return w.hasPermission(permission);
        }
        return false;
    }

    @Override
    public boolean getGlobalPermission(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        permissionsLock.readLock().lock();
        try {
            if (permissions.containsKey(permission)) {
                return permissions.get(permission);
            }
            return false;
        } finally {
            permissionsLock.readLock().unlock();
        }
    }

    @Override
    public boolean getPermission(String permission, String worldName) {
        Preconditions.checkNotNull(permission, "permission");
        Preconditions.checkNotNull(worldName, "worldName");
        if (worldDataMap.containsKey(worldName.toLowerCase())) {
            LocalGroupWorldData w = worldDataMap.get(worldName);
            return w.getPermission(permission);
        }
        return false;
    }

    @Override
    public boolean addGlobalPermission(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        boolean success;
        nodesLock.writeLock().lock();
        try {
            success = nodes.add(permissionNode);
        } finally {
            nodesLock.writeLock().unlock();
        }
        dataSource.addGlobalPermission(permissionNode);
        recalculatePermission(permissionNode);
        return success;
    }

    @Override
    public boolean addPermission(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        Preconditions.checkNotNull(worldName, "worldName");
        return getOrCreateWorld(worldName).addPermission(permissionNode);
    }

    @Override
    public boolean addBatchPermissions(NodeBatch batch) {
        Preconditions.checkNotNull(batch, "batch");
        boolean changed = false;
        ArrayList<String> changedNodes = new ArrayList<>(batch.getGlobalNodes().size());
        nodesLock.writeLock().lock();
        try {
            for (String node : batch.getGlobalNodes()) {
                node = node.toLowerCase();
                if (nodes.add(node)) {
                    changed = true;
                    changedNodes.add(node);
                }
            }
            for (String world : batch.getWorldNodes().keySet()) {
                if (getOrCreateWorld(world).addPermissions(batch.getWorldNodes().get(world))) {
                    changed = true;
                }
            }
        } finally {
            nodesLock.writeLock().unlock();
        }
        if (changed) {
            recalculatePermissions(changedNodes);
            dataSource.addGlobalPermissions(changedNodes);
        }
        return changed;
    }

    @Override
    public boolean removeGlobalPermission(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        nodesLock.writeLock().lock();
        try {
            if (nodes.remove(permissionNode.toLowerCase())) {
                recalculatePermission(permissionNode);
                dataSource.removeGlobalPermission(permissionNode);
                return true;
            }
        } finally {
            nodesLock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public boolean removePermission(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        Preconditions.checkNotNull(worldName, "worldName");
        worldName = worldName.toLowerCase();
        if (!worldDataMap.containsKey(worldName)) {
            return false;
        }
        return worldDataMap.get(worldName).removePermission(permissionNode);
    }

    @Override
    public boolean removeBatchPermissions(NodeBatch batch) {
        Preconditions.checkNotNull(batch, "batch");
        boolean changed = false;
        ArrayList<String> changedNodes = new ArrayList<>(batch.getGlobalNodes().size());
        nodesLock.writeLock().lock();
        try {
            for (String node : batch.getGlobalNodes()) {
                node = node.toLowerCase();
                if (permissions.remove(node)) {
                    changedNodes.add(node);
                    changed = true;
                }
            }
            for (String world : batch.getWorldNodes().keySet()) {
                if (worldDataMap.containsKey(world)) {
                    if (worldDataMap.get(world).addPermissions(batch.getWorldNodes().get(world))) {
                        changed = true;
                    }
                }
            }
        } finally {
            nodesLock.writeLock().unlock();
        }
        if (changed) {
            recalculatePermissions(changedNodes);
            dataSource.removeGlobalPermissions(changedNodes);
        }
        return changed;
    }

    @Override
    public NodeBatch getAllPermissions( ) {
        NodeBatch.Builder builder = new NodeBatch.Builder();
        nodesLock.readLock().lock();
        try {
            for (String node : nodes) {
                builder.addGlobalNode(node);
            }
        } finally {
            nodesLock.readLock().unlock();
        }
        for (Map.Entry<String, LocalGroupWorldData> entry : worldDataMap.entrySet()) {
            for (String node : entry.getValue().getNodes()) {
                builder.addNode(node, entry.getKey());
            }
        }
        return builder.build();
    }

    @Override
    public boolean hasGlobalTempPermission(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        permission = permission.toLowerCase();
        tempPermissionsLock.readLock().lock();
        try {
            if (!tempPermissions.containsKey(permission)) {
                return false;
            }
            return tempPermissions.get(permission);
        } finally {
            tempPermissionsLock.readLock().unlock();
        }
    }

    @Override
    public boolean hasTempPermission(String permission, String worldName) {
        Preconditions.checkNotNull(permission, "permission");
        Preconditions.checkNotNull(worldName, "worldName");
        worldName = worldName.toLowerCase();
        if (!worldDataMap.containsKey(worldName)) {
            return false;
        }
        return worldDataMap.get(worldName).hasTempPermission(permission);
    }

    @Override
    public boolean addGlobalTempPermission(String permissionNode, long time, TimeUnit unit) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        Preconditions.checkNotNull(unit, "unit");
        Preconditions.checkArgument(time > 0, "time <= 0");
        permissionNode = permissionNode.toLowerCase();
        tempNodesLock.writeLock().lock();
        boolean success;
        try {
            if (tempNodes.contains(permissionNode)) {
                return false;
            }
            success = tempNodes.add(permissionNode);
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        if (success) {
            long timeMillis = unit.toMillis(time);
            tempManager.registerGlobalTemporaryPermission(timeMillis, permissionNode, this);
            dataSource.addGlobalTempPermission(timeMillis, permissionNode);
        }
        return false;
    }

    @Override
    public boolean addTempPermissionNode(String permissionNode, String worldName, long time, TimeUnit unit) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        Preconditions.checkNotNull(worldName, "worldName");
        Preconditions.checkNotNull(unit, "unit");
        Preconditions.checkArgument(time > 0, "time <= 0");
        worldName = worldName.toLowerCase();
        if (!worldDataMap.containsKey(worldName)) {
            return false;
        }
        return worldDataMap.get(worldName).addTempPermission(permissionNode, time, unit);
    }

    @Override
    public boolean addBatchTempPermissionNodes(TemporaryNodeBatch nodes) {
        Preconditions.checkNotNull(nodes, "nodes");
        ArrayList<TemporaryPermissionEntry> changedNodes = new ArrayList<>(nodes.getGlobalNodes().size());
        tempNodesLock.writeLock().lock();
        boolean success = false;
        try {
            for (TemporaryPermissionEntry e : nodes.getGlobalNodes()) {
                if (tempNodes.add(e.getNode())) {
                    success = true;
                    changedNodes.add(e);
                }
            }
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        for (String world : nodes.getWorldNodes().keySet()) {
            if (getOrCreateWorld(world).addTempPermissions(nodes.getWorldNodes().get(world))) {
                success = true;
            }
        }
        for (TemporaryPermissionEntry e : changedNodes) {
            tempManager.registerGlobalTemporaryPermission(e.getTimeInMillis(), e.getNode(), this);
        }
        dataSource.addGlobalTempPermissions(changedNodes);
        return success;
    }

    @Override
    public boolean removeGlobalTempPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        if (tempNodes.remove(permissionNode)) {
            tempManager.cancelGlobalTemporaryPermission(permissionNode, this);
            dataSource.removeGlobalTempPermission(permissionNode);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeTempPermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        Preconditions.checkNotNull(worldName, "worldName");
        if (!worldDataMap.containsKey(worldName)) {
            return false;
        }
        return worldDataMap.get(worldName).removeTempPermission(permissionNode);
    }

    @Override
    public boolean removeBatchTempPermissionNodes(TemporaryNodeBatch nodes) {
        Preconditions.checkNotNull(nodes, "nodes");
        ArrayList<TemporaryPermissionEntry> changedNodes = new ArrayList<>(nodes.getGlobalNodes().size());
        tempNodesLock.writeLock().lock();
        boolean success = false;
        try {
            for (TemporaryPermissionEntry e : nodes.getGlobalNodes()) {
                if (tempNodes.remove(e.getNode())) {
                    success = true;
                    changedNodes.add(e);
                }
            }
        } finally {
            tempNodesLock.writeLock().unlock();
        }
        for (String world : nodes.getWorldNodes().keySet()) {
            world = world.toLowerCase();
            if (worldDataMap.containsKey(world)) {
                if (worldDataMap.get(world).removeTempPermissions(nodes.getWorldNodes().get(world))) {
                    success = true;
                }
            }
        }
        for (TemporaryPermissionEntry e : changedNodes) {
            tempManager.cancelGlobalTemporaryPermission(e.getNode(), this);
        }
        dataSource.removeGlobalTempPermissions(changedNodes);
        return success;
    }

    @Override
    public boolean hasGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        metaLock.readLock().lock();
        try {
            return meta.containsKey(key);
        } finally {
            metaLock.readLock().unlock();
        }
    }

    @Override
    public boolean hasMeta(String key, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "worldName");
        worldName = worldName.toLowerCase();
        if (!worldDataMap.containsKey(worldName)) {
            return false;
        }
        key = key.toLowerCase();
        LocalGroupWorldData world = worldDataMap.get(worldName);
        return world.hasMeta(key);
    }

    @Override
    public String getGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        key = key.toLowerCase();
        metaLock.readLock().lock();
        try {
            return meta.get(key);
        } finally {
            metaLock.readLock().unlock();
        }
    }

    @Override
    public String getMeta(String key, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "worldName");
        worldName = worldName.toLowerCase();
        if (!worldDataMap.containsKey(worldName)) {
            return null;
        }
        key = key.toLowerCase();
        LocalGroupWorldData world = worldDataMap.get(worldName);
        return world.getMeta(key);
    }

    @Override
    public void setGlobalMeta(String key, String value) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(value, "value");
        key = key.toLowerCase();
        metaLock.writeLock().lock();
        try {
            meta.put(key, value);
        } finally {
            metaLock.writeLock().unlock();
        }
        dataSource.setGlobalMeta(key, value);
    }

    @Override
    public void setMeta(String key, String value, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(value, "value");
        Preconditions.checkNotNull(worldName, "worldName");
        worldName = worldName.toLowerCase();
        if (!worldDataMap.containsKey(worldName)) {
            return;
        }
        key = key.toLowerCase();
        LocalGroupWorldData world = worldDataMap.get(worldName);
        world.setMeta(key, value);
    }

    @Override
    public boolean removeGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        boolean success;
        metaLock.writeLock().lock();
        try {
            success = meta.remove(key) != null;
        } finally {
            metaLock.writeLock().unlock();
        }
        if (success) {
            dataSource.removeGlobalMeta(key);
        }
        return success;
    }

    @Override
    public boolean removeMeta(String key, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "worldName");
        worldName = worldName.toLowerCase();
        if (!worldDataMap.containsKey(worldName)) {
            return false;
        }
        key = key.toLowerCase();
        LocalGroupWorldData world = worldDataMap.get(worldName);
        return world.removeMeta(key);
    }

    @Override
    public void setBatchMeta(MetadataBatch batch) {
        Preconditions.checkNotNull(batch, "batch");
        ArrayList<MetadataEntry> changedMeta = new ArrayList<>(batch.getGlobalNodes().size());
        metaLock.writeLock().lock();
        try {
            for (MetadataEntry e : batch.getGlobalNodes()) {
                if (e.getValue() == null) {
                    if (meta.remove(e.getKey().toLowerCase()) != null) {
                        changedMeta.add(e);
                    }
                } else {
                    meta.put(e.getKey().toLowerCase(), e.getValue());
                    changedMeta.add(e);
                }
            }
            for (String world : batch.getWorldNodes().keySet()) {
                getOrCreateWorld(world).addMetaEntries(batch.getWorldNodes().get(world));
            }
        } finally {
            metaLock.writeLock().unlock();
        }
        dataSource.setGlobalMetaEntries(changedMeta);
    }

    @Override
    public MetadataBatch getAllMetadata( ) {
        MetadataBatch.Builder builder = new MetadataBatch.Builder();
        metaLock.readLock().lock();
        try {
            for (Map.Entry<String, String> e : meta.entrySet()) {
                builder.addGlobalEntry(e.getKey(), e.getValue());
            }
        } finally {
            metaLock.readLock().unlock();
        }
        for (Map.Entry<String, LocalGroupWorldData> worldEntry : worldDataMap.entrySet()) {
            for (MetadataEntry metaEntry : worldEntry.getValue().getMetadataEntries()) {
                builder.addEntry(metaEntry.getKey(), metaEntry.getValue(), worldEntry.getKey());
            }
        }
        return builder.build();
    }

    @Override
    public int compareTo(PermissionGroup other) {
        return other.getPriority() - priority;
    }

    @Override
    public Set<? extends PermissionGroup> getParents( ) {
        return parents;
    }

    @Override
    public Set<? extends PermissionGroup> getChildren( ) {
        return children;
    }

    @Override
    public Set<? extends PermissionGroup> getAllParents( ) {
        HashSet<PermissionGroup> ret = new HashSet<>(10);
        for (PermissionGroup g : getParents()) {
            ret.add(g);
            ret.addAll(g.getAllParents());
        }
        return ret;
    }

    @Override
    public boolean addParent(PermissionGroup parent) {
        Preconditions.checkNotNull(parent, "parent");
        boolean success = parents.add(parent);
        if (success) {
            dataSource.addParent(parent);
        }
        return success;
    }

    @Override
    public boolean removeParent(PermissionGroup parent) {
        Preconditions.checkNotNull(parent, "parent");
        boolean success = parents.remove(parent);
        if (success) {
            dataSource.removeParent(parent);
        }
        return success;
    }

    @Override
    public String getName( ) {
        return name;
    }

    @Override
    public int getPriority( ) {
        priorityLock.readLock().lock();
        try {
            if (priority == -1) {
                priorityLock.readLock().unlock();
                priorityLock.writeLock().lock(); // TODO make better
                priority = dataSource.getPriority();
                priorityLock.writeLock().unlock();
                priorityLock.readLock().lock();
            }
            return priority;
        } finally {
            priorityLock.readLock().unlock();
        }
    }

    @Override
    public UUID getUniqueId( ) {
        return uniqueId;
    }
}
