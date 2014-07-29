package com.overmc.overpermissions.internal.localentities;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.overmc.overpermissions.api.GroupManager;
import com.overmc.overpermissions.api.UserManager;
import com.overmc.overpermissions.exceptions.InvalidOnlineUsernameException;
import com.overmc.overpermissions.internal.OverPermissions;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.UUIDDataSource;
import com.overmc.overpermissions.internal.datasources.UserDataSource;
import com.overmc.overpermissions.internal.datasources.UserDataSourceFactory;

public class LocalUserManager implements UserManager {
    public static final Pattern VALID_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\_]{1,16}$");
    
    private Cache<UUID, UserDataSource> dataSourceCache = CacheBuilder.newBuilder().softValues()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(Bukkit.getMaxPlayers()*2)
            .build(new CacheLoader<UUID, UserDataSource>() {
                @Override
                public UserDataSource load(UUID uuid) throws Exception {
                    return userDataSourceFactory.createUserDataSource(uuid);
                }
            }); //These are cheap, why not have a huge cache of them?

    private Cache<UUID, LocalUser> userCache = CacheBuilder.newBuilder().softValues()
            .expireAfterAccess(3, TimeUnit.MINUTES)
            .maximumSize((Bukkit.getMaxPlayers() * 5)/4) //Some server owners don't appreciate the concept of 'maximum players'
            .build(new CacheLoader<UUID, LocalUser>() {
                @Override
                public LocalUser load(UUID uuid) throws Exception {
                    LocalUser user = new LocalUser(uuid, plugin, tempManager, dataSourceCache.get(uuid));
                    user.recalculateParentData();
                    user.reloadMetadata();
                    user.reloadPermissions();
                    user.recalculatePermissions();
                    user.reloadParents(groupManager);
                    return user;
                }
            });

    private final OverPermissions plugin;
    private final GroupManager groupManager;
    private final UUIDDataSource uuidSource;
    private final TemporaryPermissionManager tempManager;
    private final UserDataSourceFactory userDataSourceFactory;

    public LocalUserManager(OverPermissions plugin, GroupManager groupManager, UUIDDataSource uuidSource, TemporaryPermissionManager tempManager, UserDataSourceFactory userDataSourceFactory) {
        this.plugin = plugin;
        this.uuidSource = uuidSource;
        this.groupManager = groupManager;
        this.tempManager = tempManager;
        this.userDataSourceFactory = userDataSourceFactory;
    }

    private UUID getUniqueId(String name) {
        try {
            return uuidSource.getOrCreateNameUuid(name);
        } catch (InvalidOnlineUsernameException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LocalUser getPermissionUser(OfflinePlayer player) {
        return getPermissionUser(player.getName());
    }

    @Override
    public LocalUser getPermissionUser(String name) {
        return userCache.getUnchecked(getUniqueId(name));
    }

    @Override
    public boolean doesUserExist(OfflinePlayer player) {
        return doesUserExist(player.getName());
    }

    @Override
    public boolean doesUserExist(String name) {
        return dataSourceCache.getUnchecked(getUniqueId(name)).doesUserExist();
    }

    @Override
    public boolean canUserExist(String name) {
        if (name.length() == 0 || name.length() > 16) { // Little performance optimization
            return false;
        }
        return VALID_USERNAME_PATTERN.matcher(name).matches();
    }

    public void initializeUser(Player player) {
        LocalUser permissionUser = getPermissionUser(player);
        permissionUser.setPlayer(player);
        if(permissionUser.getParents().size() == 0) { //Set their group to the default group if possible.
            permissionUser.addParent(groupManager.getGroup(plugin.getDefaultGroupName()));
        }
    }

    public void deinitializeUser(Player player) {
        if (userCache.asMap().containsKey(player.getUniqueId())) {
            userCache.getUnchecked(player.getUniqueId()).setPlayer(null);
        }
    }
}
