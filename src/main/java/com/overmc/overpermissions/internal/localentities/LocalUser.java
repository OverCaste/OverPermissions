package com.overmc.overpermissions.internal.localentities;

import java.util.*;
import java.util.concurrent.*;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.overmc.overpermissions.api.*;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.UserDataSource;
import com.overmc.overpermissions.internal.util.PermissionUtils;

/**
 * By definition, PermissionUsers should be treated like Bukkit's Player objects, and only names or weak references should be stored.
 */
public class LocalUser extends LocalTransientPermissionEntity implements PermissionUser {
    private final UUID uniqueId;

    private final Plugin plugin;
    private final UserDataSource userDataSource;
    private final TemporaryPermissionManager tempManager;

    private final CopyOnWriteArrayList<PermissionGroup> parents = new CopyOnWriteArrayList<>(); // These are fast for iteration, but fairly slow for modification.
    private final CopyOnWriteArrayList<PermissionGroup> allParents = new CopyOnWriteArrayList<>(); // ^

    // World specific data
    private final ConcurrentMap<String, LocalUserWorldData> worldDataMap = new ConcurrentHashMap<>();

    // The player and attachment for SuperPerms support
    private Player player;
    private PermissionAttachment attachment;
    private final Object attachmentLock = new Object();

    public LocalUser(UUID uniqueId, Plugin plugin, TemporaryPermissionManager tempManager, UserDataSource userDataSource, boolean wildcardSupport) {
        super(userDataSource, wildcardSupport);
        Preconditions.checkNotNull(uniqueId, "unique id");
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(tempManager, "temp manager");
        Preconditions.checkNotNull(userDataSource, "user data source");
        this.uniqueId = uniqueId;
        this.plugin = plugin;
        this.userDataSource = userDataSource;
        this.tempManager = tempManager;
    }

    // Utility method(s)
    public LocalUserWorldData getWorldData(String worldName) {
        Preconditions.checkNotNull(worldName, "world name");
        worldName = worldName.toLowerCase();
        if (worldDataMap.containsKey(worldName)) {
            return worldDataMap.get(worldName);
        }
        return null;
    }

    public LocalUserWorldData getOrCreateWorld(String worldName) {
        Preconditions.checkNotNull(worldName, "world name");
        worldName = worldName.toLowerCase();
        LocalUserWorldData world = worldDataMap.get(worldName);
        if (world == null) {
            world = new LocalUserWorldData(this, worldName, tempManager, userDataSource.createWorldDataSource(worldName));
            world.reloadMetadata();
            world.reloadPermissions();
            world.recalculatePermissions();
            worldDataMap.put(worldName, world);
        }
        return world;
    }

    public void reloadParents(GroupManager groupManager) {
        Preconditions.checkNotNull(groupManager, "group manager");
        parents.clear();
        for (String groupName : userDataSource.getParents()) {
            PermissionGroup group = groupManager.getGroup(groupName);
            if (group == null) {
                throw new RuntimeException("Invalid parent defined for player " + (player != null ? player.getName() : getUniqueId()) + ": " + groupName);
            }
            parents.addIfAbsent(group);
        }
        recalculateParentData();
    }

    public void recalculateParentData( ) {
        allParents.clear();
        for(PermissionGroup group : parents) {
            allParents.addIfAbsent(group);
            allParents.addAllAbsent(group.getAllParents());
        }
        
        PermissionGroup[] tempSortArray;
        tempSortArray = parents.toArray(new PermissionGroup[parents.size()]);
        Arrays.sort(tempSortArray);
        parents.clear();
        parents.addAll(Arrays.asList(tempSortArray));

        tempSortArray = allParents.toArray(new PermissionGroup[allParents.size()]);
        Arrays.sort(tempSortArray);
        allParents.clear();
        allParents.addAll(Arrays.asList(tempSortArray));

        recalculatePermissions(); // Recalculate with new parents
    }

    @Override
    public void recalculatePermission(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        super.recalculatePermission(permissionNode);
        String baseNode = PermissionUtils.getBaseNode(permissionNode);
        synchronized(attachmentLock) {
            if (attachment != null) {
                attachment.setPermission(baseNode, getPermission(baseNode, player.getWorld().getName()));
            }
        }
    }

    @Override
    public void recalculatePermissions(Iterable<String> nodes) {
        Preconditions.checkNotNull(nodes, "nodes");
        super.recalculatePermissions(nodes);

        synchronized(attachmentLock) {
            if (attachment != null) {
                for (String node : nodes) {
                    String baseNode = PermissionUtils.getBaseNode(node);
                    attachment.setPermission(baseNode, getPermission(baseNode, player.getWorld().getName()));
                }
            }
        }
    }

    @Override
    public void recalculatePermissions( ) {
        super.recalculatePermissions();
        synchronized(attachmentLock) {
            if (attachment != null) {
                for (String node : getInternalPermissionNodes()) {
                    String baseNode = PermissionUtils.getBaseNode(node);
                    attachment.setPermission(baseNode, getPermission(baseNode, player.getWorld().getName()));
                }
            }
        }
    }

    @Override
    public UUID getUniqueId( ) {
        return uniqueId;
    }

    @Override
    public boolean hasGlobalPermission(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        if (hasInternalPermission(permission)) {
            return getInternalPermission(permission);
        }
        System.out.println("Parents: " + allParents);
        for (PermissionGroup parent : allParents) {
            if (parent.hasGlobalPermission(permission)) {
                return parent.getGlobalPermission(permission);
            }
        }
        return false;
    }

    @Override
    public boolean hasPermission(String permission, String worldName) {
        Preconditions.checkNotNull(permission, "permission");
        Preconditions.checkNotNull(worldName, "world name");
        if (hasInternalPermission(permission)) { // Global player permission
            return getInternalPermission(permission);
        }
        LocalUserWorldData world = getWorldData(worldName);
        if (world != null) {
            if (world.hasInternalPermission(permission)) { // World player permission
                return world.getInternalPermission(permission);
            }
        }
        for (PermissionGroup parent : allParents) {
            if (parent.hasGlobalPermission(permission)) { // Global group permission
                return parent.getGlobalPermission(permission);
            }
            if (parent.hasPermission(permission, worldName)) { // World group permission
                return parent.getPermission(permission, worldName);
            }
        }
        return false;
    }

    @Override
    public boolean getGlobalPermission(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        if (hasInternalPermission(permission)) {
            return getInternalPermission(permission);
        }
        for (PermissionGroup parent : allParents) {
            if (parent.hasGlobalPermission(permission)) {
                return parent.getGlobalPermission(permission);
            }
        }
        return false;
    }

    @Override
    public boolean getPermission(String permission, String worldName) {
        Preconditions.checkNotNull(permission, "permission");
        Preconditions.checkNotNull(worldName, "world name");
        LocalUserWorldData world = worldDataMap.get(worldName);
        if (world != null) { // Player world permission
            if (world.hasInternalPermission(permission)) {
                return world.getInternalPermission(permission);
            }
        }
        if (hasGlobalPermission(permission)) { // Player global permission
            return getInternalPermission(permission);
        }
        for (PermissionGroup parent : allParents) {
            if (parent.hasPermission(permission, worldName)) { // Group world permission
                return parent.getPermission(permission, worldName);
            }
        }
        for (PermissionGroup parent : allParents) { // Group global permission
            if (parent.hasGlobalPermission(permission)) {
                return parent.getGlobalPermission(permission);
            }
        }
        return false;
    }

    @Override
    public boolean hasGlobalPermission(Permission permission) {
        Preconditions.checkNotNull(permission);
        return hasGlobalPermission(permission.getName());
    }

    @Override
    public boolean hasPermission(Permission permission, String worldName) {
        Preconditions.checkNotNull(permission);
        return hasPermission(permission.getName(), worldName);
    }

    @Override
    public boolean getGlobalPermission(Permission permission) {
        Preconditions.checkNotNull(permission);
        return getGlobalPermission(permission.getName());
    }

    @Override
    public boolean getPermission(Permission permission, String worldName) {
        Preconditions.checkNotNull(permission);
        return getPermission(permission.getName(), worldName);
    }

    @Override
    public boolean hasGlobalPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        return hasInternalPermissionNode(permissionNode);
    }

    @Override
    public boolean hasPermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkNotNull(worldName, "world name");
        LocalUserWorldData world = getWorldData(worldName);
        if (world == null) {
            return false;
        }
        return world.hasInternalPermissionNode(permissionNode);
    }

    @Override
    public boolean addGlobalPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        return addInternalPermissionNode(permissionNode);
    }

    @Override
    public boolean addPermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkNotNull(worldName, "world name");
        return getOrCreateWorld(worldName).addInternalPermissionNode(permissionNode);
    }

    @Override
    public boolean addBatchPermissions(NodeBatch nodes) {
        Preconditions.checkNotNull(nodes, "nodes");
        boolean success = addInternalPermissionNodes(nodes.getGlobalNodes());
        for (String worldName : nodes.getWorldNodes().keySet()) {
            if (getOrCreateWorld(worldName).addInternalPermissionNodes(nodes.getWorldNodes().get(worldName))) {
                success = true;
            }
        }
        return success;
    }

    @Override
    public boolean removeGlobalPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        return removeInternalPermissionNode(permissionNode);
    }

    @Override
    public boolean removePermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkNotNull(worldName, "world name");
        LocalUserWorldData world = getWorldData(worldName);
        if (world == null) {
            return false;
        }
        return world.removeInternalPermissionNode(permissionNode);
    }

    @Override
    public boolean removeBatchPermissions(NodeBatch nodes) {
        Preconditions.checkNotNull(nodes, "nodes");
        boolean success = removeInternalPermissionNodes(nodes.getGlobalNodes());
        for (String worldName : nodes.getWorldNodes().keySet()) {
            LocalUserWorldData world = getWorldData(worldName);
            if (world != null) {
                if (world.addInternalPermissionNodes(nodes.getWorldNodes().get(worldName))) {
                    success = true;
                }
            }
        }
        return success;
    }

    @Override
    public NodeBatch getPermissionNodes( ) {
        NodeBatch.Builder builder = NodeBatch.builder();
        for (String node : getInternalPermissionNodes()) {
            builder.addGlobalNode(node);
        }
        for (Map.Entry<String, LocalUserWorldData> entry : worldDataMap.entrySet()) {
            for (String node : entry.getValue().getInternalPermissionNodes()) {
                builder.addNode(node, entry.getKey());
            }
        }
        return builder.build();
    }

    @Override
    public Map<String, Boolean> getGlobalPermissionValues( ) {
        return getInternalPermissionValues();
    }

    @Override
    public Map<String, Boolean> getPermissionValues(String worldName) {
        LocalUserWorldData world = getWorldData(worldName);
        if (world == null) {
            return Collections.emptyMap();
        }
        return world.getInternalPermissionValues();
    }

    @Override
    public boolean hasMeta(String key, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "world name");
        key = key.toLowerCase();
        LocalUserWorldData world = getWorldData(worldName);
        if (world != null) {
            if (world.hasInternalMeta(key)) { // World user data exists
                return true;
            }
        } // Otherwise...
        if (hasInternalMeta(key)) {
            return true; // Global user meta exists
        }
        for (PermissionGroup parent : allParents) {
            if (parent.hasGlobalMeta(key)) { // Global group meta
                return true;
            }
            if (parent.hasMeta(key, worldName)) { // World group meta
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        if (hasInternalMeta(key)) {
            return true;
        }
        for (PermissionGroup parent : allParents) {
            if (parent.hasGlobalMeta(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getMeta(String key, String worldName) { // Priority is group global < group world < user global < user world
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "world name");
        key = key.toLowerCase();
        LocalUserWorldData world = getWorldData(worldName);
        if (world != null) { // If user world data exists, return that
            if (world.hasInternalMeta(key)) {
                return world.getInternalMeta(key);
            }
        } // Otherwise...
        if (hasInternalMeta(key)) { // If global user data exists, return that
            return getInternalMeta(key);
        }
        for (PermissionGroup parent : allParents) { // Then the group world meta exists
            if (parent.hasMeta(key, worldName)) {
                return parent.getMeta(key, worldName);
            }
        }
        for (PermissionGroup parent : allParents) { // And finally if group global meta exists
            if (parent.hasGlobalMeta(key)) {
                return parent.getGlobalMeta(key);
            }
        }
        return null;
    }

    @Override
    public String getGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        if (hasInternalMeta(key)) { // If global user data exists, return that
            return getInternalMeta(key);
        }
        for (PermissionGroup parent : allParents) { // Otherwise, if global group user data exists, return that
            if (parent.hasGlobalMeta(key)) {
                return parent.getGlobalMeta(key);
            }
        }
        return null;
    }

    @Override
    public void setMeta(String key, String value, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "world name");
        Preconditions.checkNotNull(value, "value");
        getOrCreateWorld(worldName).setInternalMeta(key, value);
    }

    @Override
    public void setGlobalMeta(String key, String value) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(value, "value");
        setInternalMeta(key, value);
    }

    @Override
    public boolean removeMeta(String key, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "world name");
        LocalUserWorldData world = getWorldData(worldName);
        if (world == null) {
            return false;
        }
        return world.removeInternalMeta(key);
    }

    @Override
    public boolean removeGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        return removeInternalMeta(key);
    }

    @Override
    public void setBatchMeta(MetadataBatch nodes) {
        Preconditions.checkNotNull(nodes, "nodes");
        setInternalMetaEntries(nodes.getGlobalNodes());
    }

    @Override
    public MetadataBatch getAllMetadata( ) {
        MetadataBatch.Builder builder = MetadataBatch.builder();
        for (MetadataEntry e : getInternalMetadataEntries()) {
            builder.addGlobalEntry(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, LocalUserWorldData> worldEntry : worldDataMap.entrySet()) {
            for (MetadataEntry metaEntry : worldEntry.getValue().getInternalMetadataEntries()) {
                builder.addEntry(worldEntry.getKey(), metaEntry.getKey(), metaEntry.getValue());
            }
        }
        return builder.build();
    }

    @Override
    public boolean hasGlobalTempPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        return hasInternalTempPermissionNode(permissionNode);
    }

    @Override
    public boolean hasTempPermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkNotNull(worldName, "world name");
        LocalUserWorldData world = getWorldData(worldName);
        if (world == null) {
            return false;
        }
        return world.hasInternalTempPermissionNode(permissionNode);
    }

    @Override
    public boolean addGlobalTempPermissionNode(String permissionNode, long time, TimeUnit unit) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkArgument(time > 0, "time <= 0");
        Preconditions.checkNotNull(unit, "unit");
        return addInternalTempPermissionNode(permissionNode, time, unit);
    }

    @Override
    public boolean addTempPermissionNode(String permissionNode, String worldName, long time, TimeUnit unit) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkNotNull(worldName, "world name");
        Preconditions.checkArgument(time > 0, "time <= 0");
        Preconditions.checkNotNull(unit, "unit");
        return getOrCreateWorld(worldName).addInternalTempPermissionNode(permissionNode, time, unit);
    }

    @Override
    public boolean removeGlobalTempPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        return removeInternalTempPermissionNode(permissionNode);
    }

    @Override
    public boolean removeTempPermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkNotNull(worldName, "world name");
        LocalUserWorldData world = getWorldData(worldName);
        if (world == null) {
            return false;
        }
        return world.removeInternalTempPermissionNode(permissionNode);
    }

    @Override
    public boolean addBatchTempPermissionNodes(TemporaryNodeBatch nodes) {
        Preconditions.checkNotNull(nodes, "nodes");
        boolean success = addInternalTempPermissionNodes(nodes.getGlobalNodes());
        for (String worldName : nodes.getWorldNodes().keySet()) {
            if (getOrCreateWorld(worldName).addInternalTempPermissionNodes(nodes.getWorldNodes().get(worldName))) {
                success = true;
            }
        }
        return success;
    }

    @Override
    public boolean removeBatchTempPermissionNodes(TemporaryNodeBatch nodes) {
        Preconditions.checkNotNull(nodes, "nodes");
        boolean success = removeInternalTempPermissionNodes(nodes.getGlobalNodes());
        for (String worldName : nodes.getWorldNodes().keySet()) {
            LocalUserWorldData world = getWorldData(worldName);
            if (world != null) {
                if (world.addInternalTempPermissionNodes(nodes.getWorldNodes().get(worldName))) {
                    success = true;
                }
            }
        }
        return success;
    }

    @Override
    public TemporaryNodeBatch getTempPermissionNodes( ) {
        TemporaryNodeBatch.Builder batch = TemporaryNodeBatch.builder();
        for (TemporaryPermissionEntry e : getInternalTempPermissionEntries()) {
            batch.addGlobalNode(e.getNode(), e.getExpirationTime(), TimeUnit.MILLISECONDS);
        }
        for (Map.Entry<String, LocalUserWorldData> worldEntry : worldDataMap.entrySet()) {
            for (TemporaryPermissionEntry e : worldEntry.getValue().getInternalTempPermissionEntries()) {
                batch.addNode(e.getNode(), worldEntry.getKey(), e.getExpirationTime(), TimeUnit.MILLISECONDS);
            }
        }
        return batch.build();
    }

    @Override
    public boolean addTransientPermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkNotNull(worldName, "world name");
        return getOrCreateWorld(worldName).addInternalTransientPermissionNode(permissionNode);
    }

    @Override
    public boolean addGlobalTransientPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        return addInternalTransientPermissionNode(permissionNode);
    }

    @Override
    public boolean removeTransientPermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkNotNull(worldName, "world name");
        LocalUserWorldData world = getWorldData(worldName);
        if (world == null) {
            return false;
        }
        return world.removeInternalTransientPermissionNode(permissionNode);
    }

    @Override
    public boolean removeGlobalTransientPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        return removeInternalTransientPermissionNode(permissionNode);
    }

    @Override
    public boolean hasGlobalTransientPermissionNode(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        return hasInternalTransientPermissionNode(permissionNode);
    }

    @Override
    public boolean hasTransientPermissionNode(String permissionNode, String worldName) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkNotNull(worldName, "world name");
        LocalUserWorldData world = getWorldData(worldName);
        if (world == null) {
            return false;
        }
        return world.hasInternalTransientPermissionNode(permissionNode);
    }

    @Override
    public NodeBatch getTransientPermissionNodes( ) {
        NodeBatch.Builder builder = NodeBatch.builder();
        for (String node : getInternalTransientNodes()) {
            builder.addGlobalNode(node);
        }
        for (Map.Entry<String, LocalUserWorldData> entry : worldDataMap.entrySet()) {
            for (String node : entry.getValue().getInternalTransientNodes()) {
                builder.addNode(node, entry.getKey());
            }
        }
        return builder.build();
    }

    @Override
    public Set<PermissionGroup> getAllParents( ) {
        return Sets.newTreeSet(allParents); // Defensive copy, tree sets are ordered, which is mandated for this method.
    }

    @Override
    public Set<PermissionGroup> getParents( ) {
        return Sets.newTreeSet(parents); // ^
    }

    @Override
    public boolean addParent(PermissionGroup parent) {
        Preconditions.checkNotNull(parent, "parent");
        boolean success = parents.addIfAbsent(parent);
        if (success) {
            userDataSource.addParent(parent);
            if(parent instanceof LocalGroup) {
                ((LocalGroup)parent).addUserToGroup(this);
            } else {
                plugin.getLogger().warning("Group can't have a user added to it, it's not a LocalGroup: " + parent.getName() + "(" + parent.getClass().getName() + ")");
            }
            recalculateParentData();
        }
        return success;
    }

    @Override
    public boolean removeParent(PermissionGroup parent) {
        Preconditions.checkNotNull(parent, "parent");
        boolean success = parents.remove(parent);
        if (success) {
            userDataSource.removeParent(parent);
            if(parent instanceof LocalGroup) {
                ((LocalGroup)parent).removeUserFromGroup(this);
            } else {
                plugin.getLogger().warning("Group can't have a user removed from it, it's not a LocalGroup: " + parent.getName() + "(" + parent.getClass().getName() + ")");
            }
            recalculateParentData();
        }
        return success;
    }

    @Override
    public void setParent(PermissionGroup parent) {
        Preconditions.checkNotNull(parent, "parent");
        for(PermissionGroup g : parents) {
            if(g instanceof LocalGroup) {
                ((LocalGroup)g).removeUserFromGroup(this);
            } else {
                plugin.getLogger().warning("Group can't have a user removed from it, it's not a LocalGroup: " + g.getName() + "(" + g.getClass().getName() + ")");
            }
        }
        if(parent instanceof LocalGroup) {
            ((LocalGroup)parent).addUserToGroup(this);
        } else {
            plugin.getLogger().warning("Group can't have a user added to it, it's not a LocalGroup: " + parent.getName() + "(" + parent.getClass().getName() + ")");
        }
        parents.clear();
        parents.addIfAbsent(parent);
        userDataSource.setParent(parent);
        recalculateParentData();
    }

    @Override
    protected void registerTempPermission(String node, long timeInMillis) {
        tempManager.registerGlobalTemporaryPermission(this, new TemporaryPermissionEntry(node, System.currentTimeMillis() + timeInMillis));
    }

    @Override
    protected void cancelTempPermission(String node) {
        tempManager.cancelGlobalTemporaryPermission(this, node);
    }

    public synchronized void setPlayer(Player player) {
        synchronized(attachmentLock) {
            this.player = player;
            if (this.attachment != null) {
                this.attachment.remove();
            }
            if (player == null) {
                this.attachment = null;
            } else {
                this.attachment = player.addAttachment(plugin);
            }
        }
    }
}
