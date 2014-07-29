package com.overmc.overpermissions.internal.localentities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.overmc.overpermissions.api.GroupManager;
import com.overmc.overpermissions.api.MetadataBatch;
import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.NodeBatch;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.api.TemporaryNodeBatch;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;
import com.overmc.overpermissions.internal.PermissionUtils;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.UserDataSource;

public class LocalUser extends LocalTransientPermissionEntity implements PermissionUser {
    private final UUID uniqueId;

    private final Plugin plugin;
    private final UserDataSource userDataSource;
    private final TemporaryPermissionManager tempManager;

    private final Set<PermissionGroup> parents = new CopyOnWriteArraySet<>(); // These are fast for iteration, but fairly slow for modification.
    private final Set<String> allParentNodes = new CopyOnWriteArraySet<>();
    private final Map<String, String> parentMetadataMap = new ConcurrentHashMap<>();

    // World specific data
    private final ConcurrentMap<String, LocalUserWorldData> worldDataMap = new ConcurrentHashMap<>();

    // The player and attachment for SuperPerms support
    private Player player;
    private PermissionAttachment attachment;
    private final Lock attachmentLock = new ReentrantLock();

    public LocalUser(UUID uniqueId, Plugin plugin, TemporaryPermissionManager tempManager, UserDataSource userDataSource) {
        super(userDataSource);
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
            parents.add(group);
        }
        recalculateParentData();
    }

    public void recalculateParentData( ) {
        Set<String> newParentNodes = new HashSet<>();
        Multimap<String, String> worldNewParentNodes = HashMultimap.create();
        Multimap<String, MetadataEntry> worldGroupMetaEntries = HashMultimap.create();
        ArrayList<PermissionGroup> sortedParents = new ArrayList<>(parents);
        HashMap<String, String> newParentMeta = new HashMap<>();
        Collections.sort(sortedParents);
        for (PermissionGroup g : sortedParents) {
            NodeBatch nodeBatch = g.getPermissionNodes();
            MetadataBatch metaBatch = g.getAllMetadata();
            for (String node : nodeBatch.getGlobalNodes()) {
                newParentNodes.add(PermissionUtils.getBaseNode(node).toLowerCase());
            }
            for (MetadataEntry e : metaBatch.getGlobalNodes()) {
                newParentMeta.put(e.getKey(), e.getValue());
            }
            for (String worldName : nodeBatch.getWorldNodes().keySet()) {
                String lowerCaseWorldName = worldName.toLowerCase();
                for (String node : nodeBatch.getWorldNodes().get(worldName)) {
                    worldNewParentNodes.put(lowerCaseWorldName, node.toLowerCase());
                }
            }
            for (String worldName : metaBatch.getWorldNodes().keySet()) {
                String lowerCaseWorldName = worldName.toLowerCase();
                for (MetadataEntry entry : metaBatch.getWorldNodes().get(worldName)) {
                    worldGroupMetaEntries.put(lowerCaseWorldName, entry);
                }
            }
        }
        allParentNodes.clear();
        allParentNodes.addAll(newParentNodes);
        parentMetadataMap.clear();
        parentMetadataMap.putAll(newParentMeta);
        for (String worldName : worldNewParentNodes.keySet()) {
            LocalUserWorldData world = getOrCreateWorld(worldName);
            world.clearGroupNodes();
            world.addGroupNodes(worldNewParentNodes.get(worldName));
        }
        for (String worldName : worldGroupMetaEntries.keySet()) {
            LocalUserWorldData world = getOrCreateWorld(worldName);
            world.clearGroupMeta();
            world.addGroupMeta(worldGroupMetaEntries.get(worldName));
        }
        recalculatePermissions(); // Recalculate with new parents
    }

    @Override
    protected Set<String> getAllNodes( ) {
        return Sets.union(super.getAllNodes(), allParentNodes);
    }

    @Override
    public void recalculatePermission(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        super.recalculatePermission(permissionNode);
        String baseNode = PermissionUtils.getBaseNode(permissionNode);
        attachmentLock.lock();
        try {
            attachment.setPermission(baseNode, getPermission(baseNode, player.getWorld().getName()));
        } finally {
            attachmentLock.unlock();
        }
    }

    @Override
    public void recalculatePermissions(Iterable<String> nodes) {
        Preconditions.checkNotNull(nodes, "nodes");
        super.recalculatePermissions(nodes);

        attachmentLock.lock();
        try {
            for (String node : nodes) {
                String baseNode = PermissionUtils.getBaseNode(node);
                attachment.setPermission(baseNode, getPermission(baseNode, player.getWorld().getName()));
            }
        } finally {
            attachmentLock.unlock();
        }
    }

    @Override
    public void recalculatePermissions( ) {
        super.recalculatePermissions();
        attachmentLock.lock();
        try {
            if (attachment != null) {
                for (String node : getInternalPermissionNodes()) {
                    String baseNode = PermissionUtils.getBaseNode(node);
                    attachment.setPermission(baseNode, getPermission(baseNode, player.getWorld().getName()));
                }
            }
        } finally {
            attachmentLock.unlock();
        }
    }

    @Override
    public UUID getUniqueId( ) {
        return uniqueId;
    }

    @Override
    public boolean hasGlobalPermission(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        return hasInternalPermission(permission);
    }

    @Override
    public boolean hasPermission(String permission, String worldName) {
        Preconditions.checkNotNull(permission, "permission");
        Preconditions.checkNotNull(worldName, "world name");
        if (hasGlobalPermission(permission)) {
            return true;
        }
        LocalUserWorldData world = getWorldData(worldName);
        if (world == null) {
            return false;
        }
        return world.hasInternalPermission(permission);
    }

    @Override
    public boolean getGlobalPermission(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        return getInternalPermission(permission);
    }

    @Override
    public boolean getPermission(String permission, String worldName) {
        Preconditions.checkNotNull(permission, "permission");
        Preconditions.checkNotNull(worldName, "world name");
        boolean value = false;
        if (hasGlobalPermission(permission)) {
            value = getInternalPermission(permission);
        }
        LocalUserWorldData world = worldDataMap.get(worldName);
        if (world != null) { // World permissions override global ones.
            if (world.hasInternalPermission(permission)) {
                value = world.getInternalPermission(permission);
            }
        }
        return value;
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
    public boolean hasMeta(String key, String worldName) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(worldName, "world name");
        key = key.toLowerCase();
        LocalUserWorldData world = getWorldData(worldName);
        if (world != null) {
            if (world.hasInternalMeta(key)) { // World user data exists
                return true;
            }
            if (world.hasGroupMeta(key)) { // World group data exists
                return true;
            }
        } // Otherwise...
        if (hasInternalMeta(key)) {
            return true; // Global user meta exists
        }
        if (parentMetadataMap.containsKey(key)) {
            return true; // Global group meta exists
        }
        return false;
    }

    @Override
    public boolean hasGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        return hasInternalMeta(key);
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
        if (world != null) { // Then the group world meta exists
            if (world.hasGroupMeta(key)) {
                return world.getGroupMeta(key);
            }
        }
        if (parentMetadataMap.containsKey(key)) { // And finally if group global meta exists
            return parentMetadataMap.get(key);
        }
        return null;
    }

    @Override
    public String getGlobalMeta(String key) {
        Preconditions.checkNotNull(key, "key");
        if (hasInternalMeta(key)) { // If global user data exists, return that
            return getInternalMeta(key);
        }
        if (parentMetadataMap.containsKey(key)) { // Otherwise, if global group user data exists, return that
            return parentMetadataMap.get(key);
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
        HashSet<PermissionGroup> ret = new HashSet<>();
        for (PermissionGroup g : parents) {
            ret.add(g);
            ret.addAll(g.getParents());
        }
        return ret;
    }

    @Override
    public Set<PermissionGroup> getParents( ) {
        return new HashSet<>(parents); // Defensive copy
    }

    @Override
    public boolean addParent(PermissionGroup parent) {
        Preconditions.checkNotNull(parent, "parent");
        boolean success = parents.add(parent);
        if (success) {
            userDataSource.addParent(parent);
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
            recalculateParentData();
        }
        return success;
    }

    @Override
    public void setParent(PermissionGroup parent) {
        Preconditions.checkNotNull(parent, "parent");
        parents.clear();
        parents.add(parent);
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
        attachmentLock.lock();
        try {
            this.player = player;
            if (this.attachment != null) {
                this.attachment.remove();
            }
            if (player == null) {
                this.attachment = null;
            } else {
                this.attachment = player.addAttachment(plugin);
            }
            if (player == null) {
                for (PermissionGroup g : getAllParents()) {
                    if (g instanceof LocalGroup) {
                        ((LocalGroup) g).removeUserFromGroup(this);
                    } else {
                        plugin.getLogger().warning("Unrecognized group type while removing a player from groups: (" + g + ")"); // LocalGroups are the only ones recognized, maybe more versatile support in the future?
                    }
                }
            } else {
                for (PermissionGroup g : getAllParents()) {
                    if (g instanceof LocalGroup) {
                        ((LocalGroup) g).addUserToGroup(this);
                    } else {
                        plugin.getLogger().warning("Unrecognized group type while adding a player to groups: (" + g + ")"); // ^
                    }
                }
            }
        } finally {
            attachmentLock.unlock();
        }
    }
}
