package com.overmc.overpermissions.internal;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.collect.*;
import com.overmc.overpermissions.api.*;
import com.overmc.overpermissions.internal.datasources.TemporaryPermissionEntityDataSource;
import com.overmc.overpermissions.internal.datasources.TemporaryPermissionEntityDataSourceFactory;

public class TemporaryPermissionManager {
    private final OverPermissions plugin;

    private final ConcurrentMap<GlobalNodeKey, Integer> globalTaskIdMap = new ConcurrentHashMap<>();
    private final Multimap<UUID, GlobalNodeKey> entityGlobalTaskMap = Multimaps.synchronizedMultimap(HashMultimap.<UUID, GlobalNodeKey> create());

    private final ConcurrentMap<WorldNodeKey, Integer> worldTaskIdMap = new ConcurrentHashMap<>();
    private final Multimap<UUID, WorldNodeKey> entityWorldTaskMap = Multimaps.synchronizedMultimap(HashMultimap.<UUID, WorldNodeKey> create());

    private final TemporaryPermissionEntityDataSourceFactory sourceFactory;

    public TemporaryPermissionManager(OverPermissions plugin, TemporaryPermissionEntityDataSourceFactory sourceFactory) {
        this.plugin = plugin;
        this.sourceFactory = sourceFactory;
    }

    // ms/s = 1000/1
    // ticks/s = 20/1
    private long convertMillisToTicks(long millis) {
        return Math.max(0L, (millis / 1000L) * 20L);
    }

    public void registerGlobalTemporaryPermission(final TemporaryPermissionEntity entity, TemporaryPermissionEntry entry) {
        long deltaTime = entry.getExpirationTime() - System.currentTimeMillis();
        final GlobalNodeKey globalKey = new GlobalNodeKey(entity.getUniqueId(), entry.getNode());
        final WeakReference<TemporaryPermissionEntry> entryReference = new WeakReference<>(entry);

        BukkitRunnable runnable = (new BukkitRunnable() {
            @Override
            public void run( ) {
                TemporaryPermissionEntry entry = entryReference.get();
                if (entry == null) {
                    plugin.getLogger().warning("Tried to process a node for an entity that doesn't exist anymore! Make sure to call cancelTemporaryPermissions!");
                } else {
                    entity.removeGlobalTempPermissionNode(entry.getNode());
                }
                if (!globalTaskIdMap.remove(globalKey, getTaskId())) {
                    plugin.getLogger().warning("Tried to cancel an unknown task: " + getTaskId());
                }
            }
        });

        BukkitTask task = runnable.runTaskLater(plugin, convertMillisToTicks(deltaTime));
        globalTaskIdMap.put(globalKey, task.getTaskId());
        entityGlobalTaskMap.put(entity.getUniqueId(), globalKey);
    }

    public boolean cancelGlobalTemporaryPermission(UniqueEntity entity, String node) {
        final GlobalNodeKey globalKey = new GlobalNodeKey(entity.getUniqueId(), node);
        if (globalTaskIdMap.remove(globalKey) != null) {
            entityGlobalTaskMap.remove(entity.getUniqueId(), globalKey);
            return true;
        }
        return false;
    }

    public void registerWorldTemporaryPermission(final TemporaryPermissionEntity entity, final String worldName, TemporaryPermissionEntry entry) {
        long deltaTime = entry.getExpirationTime() - System.currentTimeMillis();
        final WorldNodeKey worldKey = new WorldNodeKey(entity.getUniqueId(), worldName, entry.getNode());
        final WeakReference<TemporaryPermissionEntry> entryReference = new WeakReference<>(entry);

        BukkitRunnable runnable = (new BukkitRunnable() {
            @Override
            public void run( ) {
                TemporaryPermissionEntry entry = entryReference.get();
                if (entry == null) {
                    plugin.getLogger().warning("Tried to process a node for an entity that doesn't exist anymore! Make sure to call cancelTemporaryPermissions!");
                } else {
                    entity.removeTempPermissionNode(entry.getNode(), worldName);
                }
                if (!worldTaskIdMap.remove(worldKey, getTaskId())) {
                    plugin.getLogger().warning("Tried to cancel an unknown task: " + getTaskId());
                }
            }
        });

        BukkitTask task = runnable.runTaskLater(plugin, convertMillisToTicks(deltaTime));
        worldTaskIdMap.put(worldKey, task.getTaskId());
        entityWorldTaskMap.put(entity.getUniqueId(), worldKey);
    }

    public boolean cancelWorldTemporaryPermission(UniqueEntity entity, String worldName, String node) {
        final WorldNodeKey worldKey = new WorldNodeKey(entity.getUniqueId(), worldName, node);
        if (worldTaskIdMap.remove(worldKey) != null) {
            entityWorldTaskMap.remove(entity.getUniqueId(), worldKey);
            return true;
        }
        return false;
    }

    public void cancelTemporaryPermissions(UniqueEntity entity) {
        for (GlobalNodeKey key : entityGlobalTaskMap.removeAll(entity.getUniqueId())) {
            plugin.getServer().getScheduler().cancelTask(globalTaskIdMap.remove(key).intValue());
        }
        for (WorldNodeKey key : entityWorldTaskMap.removeAll(entity.getUniqueId())) {
            plugin.getServer().getScheduler().cancelTask(worldTaskIdMap.remove(key).intValue());
        }
    }

    private void initTempPerms(TemporaryPermissionEntity entity, TemporaryPermissionEntityDataSource source) {
        TemporaryNodeBatch tempNodes = source.getTempPermissions();
        for (TemporaryPermissionEntry e : tempNodes.getGlobalNodes()) {
            registerGlobalTemporaryPermission(entity, e);
        }
        for (String worldName : tempNodes.getWorldNodes().keySet()) {
            for (TemporaryPermissionEntry e : tempNodes.getWorldNodes().get(worldName)) {
                registerWorldTemporaryPermission(entity, worldName, e);
            }
        }
    }

    public void initializeGroupTemporaryPermissions(PermissionGroup group) {
        TemporaryPermissionEntityDataSource source = sourceFactory.createTempGroupDataSource(group.getName());
        initTempPerms(group, source);
    }

    public void initializePlayerTemporaryPermissions(PermissionUser user) {
        TemporaryPermissionEntityDataSource source = sourceFactory.createTempPlayerDataSource(user.getUniqueId());
        initTempPerms(user, source);
    }

    /**
     * A class representing a global node and owner.
     */
    private class GlobalNodeKey {
        private final UUID uniqueId;
        private final String node;

        public GlobalNodeKey(UUID uniqueId, String node) {
            this.uniqueId = uniqueId;
            this.node = node;
        }

        @Override
        public int hashCode( ) {
            return Objects.hash(uniqueId, node);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof GlobalNodeKey)) {
                return false;
            }
            GlobalNodeKey otherKey = (GlobalNodeKey) obj;
            return Objects.equals(otherKey.uniqueId, uniqueId) && Objects.equals(otherKey.node, node);
        }
    }

    /**
     * A class representing a world node, world, and owner.
     */
    private class WorldNodeKey {
        private final UUID uniqueId;
        private final String worldName;
        private final String node;

        public WorldNodeKey(UUID uniqueId, String worldName, String node) {
            this.uniqueId = uniqueId;
            this.worldName = worldName;
            this.node = node;
        }

        @Override
        public int hashCode( ) {
            return Objects.hash(uniqueId, worldName, node);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof WorldNodeKey)) {
                return false;
            }
            WorldNodeKey otherKey = (WorldNodeKey) obj;
            return Objects.equals(otherKey.uniqueId, uniqueId) && Objects.equals(otherKey.worldName, worldName) && Objects.equals(otherKey.node, node);
        }
    }
}
