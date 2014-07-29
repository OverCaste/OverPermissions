package com.overmc.overpermissions.internal.localentities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.overmc.overpermissions.api.GroupManager;
import com.overmc.overpermissions.api.MetadataBatch;
import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.NodeBatch;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.TemporaryNodeBatch;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.GroupDataSource;

/**
 * A class that represents a ram stored version of a database group.
 */
public class LocalGroup extends LocalPermissionEntity implements PermissionGroup {
    private final GroupDataSource groupDataSource;
    private final TemporaryPermissionManager tempManager;

    private final String name;
    private final UUID uniqueId;
    private int priority;
    private final Set<PermissionGroup> parents = new CopyOnWriteArraySet<>(); // These are fast for iteration, but fairly slow for modification.
    private final Set<PermissionGroup> children = new CopyOnWriteArraySet<>();

    // World specific data
    private final ConcurrentMap<String, LocalGroupWorldData> worldDataMap = new ConcurrentHashMap<>();

    // Concurrency locks
    private final ReadWriteLock priorityLock = new ReentrantReadWriteLock();

    private final Set<LocalUser> playersInGroup = Collections.newSetFromMap(new ConcurrentHashMap<LocalUser, Boolean>());

    public LocalGroup(GroupDataSource groupSource, TemporaryPermissionManager tempManager, String name, int priority) {
        super(groupSource);
        Preconditions.checkNotNull(groupSource, "groupSource");
        Preconditions.checkNotNull(tempManager, "tempManager");
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkArgument(priority >= 0, "Priority must be greater than or equal to 0.");
        this.groupDataSource = groupSource;
        this.tempManager = tempManager;
        this.name = name;
        this.uniqueId = UUID.nameUUIDFromBytes(("LocalGroup:" + name).getBytes(Charsets.UTF_8));
        this.priority = priority;
    }

    // Utility method(s)
    protected LocalGroupWorldData getWorldData(String worldName) {
        Preconditions.checkNotNull(worldName, "worldName");
        worldName = worldName.toLowerCase();
        if (worldDataMap.containsKey(worldName)) {
            return worldDataMap.get(worldName);
        }
        return null;
    }

    private LocalGroupWorldData getOrCreateWorld(String worldName) {
        Preconditions.checkNotNull(worldName, "worldName");
        worldName = worldName.toLowerCase();
        LocalGroupWorldData world = worldDataMap.get(worldName);
        if (world == null) {
            world = new LocalGroupWorldData(this, worldName, tempManager, groupDataSource.createWorldDataSource(worldName));
            world.reloadMetadata();
            world.reloadPermissions();
            world.recalculatePermissions();
            worldDataMap.put(worldName, world);
        }
        return world;
    }

    public void reloadWorldPermissions( ) {
        for (LocalGroupWorldData world : worldDataMap.values()) {
            world.reloadPermissions();
            world.recalculatePermissions();
        }
    }

    public void reloadWorldMetadata( ) {
        for (LocalGroupWorldData world : worldDataMap.values()) {
            world.reloadMetadata();
        }
    }

    public void reloadParentsAndChildren(GroupManager groupManager) {
        Collection<String> newParentNames = groupDataSource.getParents();
        Collection<PermissionGroup> newParents = new ArrayList<PermissionGroup>(newParentNames.size());
        for (String name : newParentNames) {
            PermissionGroup g = groupManager.getGroup(name);
            if(g == null) {
                throw new RuntimeException("There was an invalid group parent set for a group! (group=" + this.name + ", parent=" + name + "), groups: (" + groupManager.getGroups() + ")");
            }
            newParents.add(g);
        }
        Collection<String> newChildrenNames = groupDataSource.getChildren();
        Collection<PermissionGroup> newChildren = new ArrayList<PermissionGroup>(newChildrenNames.size());
        for (String name : newChildrenNames) {
            PermissionGroup g = groupManager.getGroup(name);
            if(g == null) {
                throw new RuntimeException("There was an invalid group child set for a group! (group=" + this.name + ", child=" + name + "), groups: (" + groupManager.getGroups() + ")");
            }
            newChildren.add(g);
        }
        parents.clear();
        parents.addAll(newParents);
        children.clear();
        children.addAll(newChildren);
    }

    // Recalculating permissions will also recalculate the permissions of all players in this group, to instantly propagate the change.
    // Metadata is automatically checked. There isn't a proper 'store.'
    @Override
    protected void recalculatePermission(String node) {
        super.recalculatePermission(node);
        for (LocalUser u : playersInGroup) {
            u.recalculatePermission(node);
        }
    }

    @Override
    protected void recalculatePermissions(Iterable<String> nodes) {
        super.recalculatePermissions(nodes);
        for (LocalUser u : playersInGroup) {
            u.recalculatePermissions(nodes);
        }
    }

    @Override
    protected void recalculatePermissions( ) {
        super.recalculatePermissions();
        for (LocalUser u : playersInGroup) {
            u.recalculatePermissions();
        }
    }

    @Override
    public boolean hasGlobalPermission(String permission) {
        return hasInternalPermission(permission);
    }

    @Override
    public boolean hasPermission(String permission, String worldName) {
        LocalGroupWorldData w = getWorldData(worldName);
        if (w != null) {
            return w.hasInternalPermission(permission);
        }
        return false;
    }

    @Override
    public boolean hasGlobalPermissionNode(String permissionNode) {
        return hasInternalPermissionNode(permissionNode);
    }

    @Override
    public boolean hasPermissionNode(String permissionNode, String worldName) {
        LocalGroupWorldData w = getWorldData(worldName);
        if (w == null) {
            return false;
        }
        return w.hasInternalPermissionNode(permissionNode);
    }

    @Override
    public TemporaryNodeBatch getTempPermissionNodes( ) {
        TemporaryNodeBatch.Builder builder = TemporaryNodeBatch.builder();
        for (TemporaryPermissionEntry e : getInternalTempPermissionEntries()) {
            builder.addGlobalNode(e.getNode(), e.getExpirationTime(), TimeUnit.MILLISECONDS);
        }
        for (Map.Entry<String, LocalGroupWorldData> entry : worldDataMap.entrySet()) {
            for (TemporaryPermissionEntry e : entry.getValue().getInternalTempPermissionEntries()) {
                builder.addNode(e.getNode(), entry.getKey(), e.getExpirationTime(), TimeUnit.MILLISECONDS);
            }
        }
        return builder.build();
    }

    @Override
    public boolean getGlobalPermission(String permission) {
        return getInternalPermission(permission);
    }

    @Override
    public boolean getPermission(String permission, String worldName) {
        LocalGroupWorldData w = getWorldData(worldName);
        if (w == null) {
            return false;
        }
        return w.getInternalPermission(permission);
    }

    @Override
    public boolean addGlobalPermissionNode(String permissionNode) {
        return addInternalPermissionNode(permissionNode);
    }

    @Override
    public boolean addPermissionNode(String permissionNode, String worldName) {
        return getOrCreateWorld(worldName).addInternalPermissionNode(permissionNode);
    }

    @Override
    public boolean addBatchPermissions(NodeBatch batch) {
        Preconditions.checkNotNull(batch, "batch");
        boolean changed = false;
        super.addInternalPermissionNodes(batch.getGlobalNodes());
        if (changed) {
            recalculatePermissions(batch.getGlobalNodes());
        }
        return changed;
    }

    @Override
    public boolean removeGlobalPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        if (super.removeInternalPermissionNode(permissionNode)) {
            recalculatePermission(permissionNode);
            return true;
        }
        return false;
    }

    @Override
    public boolean removePermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        Preconditions.checkNotNull(worldName, "worldName");
        worldName = worldName.toLowerCase();
        if (!worldDataMap.containsKey(worldName)) {
            return false;
        }
        return worldDataMap.get(worldName).removeInternalPermissionNode(permissionNode);
    }

    @Override
    public boolean removeBatchPermissions(NodeBatch batch) {
        Preconditions.checkNotNull(batch, "batch");
        boolean changed = super.removeInternalPermissionNodes(batch.getGlobalNodes());
        for (String world : batch.getWorldNodes().keySet()) {
            if (worldDataMap.containsKey(world)) {
                if (worldDataMap.get(world).removeInternalPermissionNodes(batch.getWorldNodes().get(world))) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Override
    public NodeBatch getPermissionNodes( ) {
        NodeBatch.Builder builder = NodeBatch.builder();
        for (String node : getInternalPermissionNodes()) {
            builder.addGlobalNode(node);
        }
        for (Map.Entry<String, LocalGroupWorldData> entry : worldDataMap.entrySet()) {
            for (String node : entry.getValue().getInternalPermissionNodes()) {
                builder.addNode(node, entry.getKey());
            }
        }
        return builder.build();
    }

    @Override
    public boolean hasGlobalTempPermissionNode(String permission) {
        return hasInternalTempPermissionNode(permission);
    }

    @Override
    public boolean hasTempPermissionNode(String permission, String worldName) {
        LocalGroupWorldData w = getWorldData(worldName);
        if (w != null) {
            return worldDataMap.get(worldName).hasInternalTempPermissionNode(permission);
        }
        return false;
    }

    @Override
    public boolean addGlobalTempPermissionNode(String permissionNode, long time, TimeUnit unit) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        Preconditions.checkNotNull(unit, "unit");
        Preconditions.checkArgument(time > 0, "time <= 0");
        return addInternalTempPermissionNode(permissionNode, time, unit);
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
        return worldDataMap.get(worldName).addInternalTempPermissionNode(permissionNode, time, unit);
    }

    @Override
    public boolean addBatchTempPermissionNodes(TemporaryNodeBatch batch) {
        Preconditions.checkNotNull(batch, "nodes");
        boolean changed = super.addInternalTempPermissionNodes(batch.getGlobalNodes());
        for (String world : batch.getWorldNodes().keySet()) {
            if (worldDataMap.containsKey(world)) {
                if (worldDataMap.get(world).addInternalTempPermissionNodes(batch.getWorldNodes().get(world))) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Override
    public boolean removeGlobalTempPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        return removeInternalTempPermissionNode(permissionNode);
    }

    @Override
    public boolean removeTempPermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permissionNode");
        Preconditions.checkNotNull(worldName, "worldName");
        if (!worldDataMap.containsKey(worldName)) {
            return false;
        }
        return worldDataMap.get(worldName).removeInternalTempPermissionNode(permissionNode);
    }

    @Override
    public boolean removeBatchTempPermissionNodes(TemporaryNodeBatch batch) {
        Preconditions.checkNotNull(batch, "batch");
        boolean changed = super.removeInternalTempPermissionNodes(batch.getGlobalNodes());
        for (String world : batch.getWorldNodes().keySet()) {
            if (worldDataMap.containsKey(world)) {
                if (worldDataMap.get(world).removeInternalTempPermissionNodes(batch.getWorldNodes().get(world))) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Override
    public boolean hasGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        return hasInternalMeta(key);
    }

    @Override
    public boolean hasMeta(String key, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "worldName");
        LocalGroupWorldData world = getWorldData(worldName);
        if (world == null) {
            return false;
        }
        return world.hasInternalMeta(key);
    }

    @Override
    public String getGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        return getInternalMeta(key);
    }

    @Override
    public String getMeta(String key, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "worldName");
        LocalGroupWorldData world = getWorldData(worldName);
        if (world == null) {
            return null;
        }
        return world.getInternalMeta(key);
    }

    @Override
    public void setGlobalMeta(String key, String value) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(value, "value");
        setInternalMeta(key, value);
    }

    @Override
    public void setMeta(String key, String value, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(value, "value");
        Preconditions.checkNotNull(worldName, "worldName");
        LocalGroupWorldData world = getWorldData(worldName);
        if (world == null) {
            return;
        }
        world.setInternalMeta(key, value);
    }

    @Override
    public boolean removeGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        return removeInternalMeta(key);
    }

    @Override
    public boolean removeMeta(String key, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "worldName");
        LocalGroupWorldData world = getWorldData(worldName);
        if (world == null) {
            return false;
        }
        return world.removeInternalMeta(key);
    }

    @Override
    public void setBatchMeta(MetadataBatch batch) {
        Preconditions.checkNotNull(batch, "batch");
        setInternalMetaEntries(batch.getGlobalNodes());
        for (String world : batch.getWorldNodes().keySet()) {
            getOrCreateWorld(world).addInternalMetaEntries(batch.getWorldNodes().get(world));
        }
    }

    @Override
    public MetadataBatch getAllMetadata( ) {
        MetadataBatch.Builder builder = MetadataBatch.builder();
        for (MetadataEntry e : getInternalMetadataEntries()) {
            builder.addGlobalEntry(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, LocalGroupWorldData> worldEntry : worldDataMap.entrySet()) {
            for (MetadataEntry metaEntry : worldEntry.getValue().getInternalMetadataEntries()) {
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
    public Set<PermissionGroup> getParents( ) {
        return parents;
    }

    @Override
    public Set<PermissionGroup> getChildren( ) {
        return children;
    }

    @Override
    public Set<PermissionGroup> getAllParents( ) {
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
            groupDataSource.addParent(parent);
        }
        return success;
    }

    @Override
    public boolean removeParent(PermissionGroup parent) {
        Preconditions.checkNotNull(parent, "parent");
        boolean success = parents.remove(parent);
        if (success) {
            groupDataSource.removeParent(parent);
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
                priority = groupDataSource.getPriority();
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

    @Override
    protected void registerTempPermission(String node, long timeInMillis) {
        tempManager.registerGlobalTemporaryPermission(this, new TemporaryPermissionEntry(node, System.currentTimeMillis() + timeInMillis));
    }

    @Override
    protected void cancelTempPermission(String node) {
        tempManager.cancelGlobalTemporaryPermission(this, node);
    }

    void addUserToGroup(LocalUser user) {
        playersInGroup.add(user);
    }

    void removeUserFromGroup(LocalUser user) {
        playersInGroup.remove(user);
    }
}
