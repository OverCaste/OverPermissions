package com.overmc.overpermissions.internal.localentities;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.overmc.overpermissions.api.GroupManager;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.UserManager;
import com.overmc.overpermissions.exceptions.InvalidOnlineUsernameException;
import com.overmc.overpermissions.exceptions.InvalidUsernameException;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.UUIDHandler;
import com.overmc.overpermissions.internal.datasources.UserDataSource;
import com.overmc.overpermissions.internal.datasources.UserDataSourceFactory;

public class LocalUserManager implements UserManager {
    public static final Pattern VALID_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\_]{1,16}$");

    private LoadingCache<UUID, UserDataSource> dataSourceCache = CacheBuilder.newBuilder()
            .softValues()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(Bukkit.getMaxPlayers() * 2)
            .build(new CacheLoader<UUID, UserDataSource>() {
                @Override
                public UserDataSource load(UUID uuid) throws Exception {
                    return userDataSourceFactory.createUserDataSource(uuid);
                }
            }); // These are cheap, why not have a huge cache of them?

    private LoadingCache<UUID, LocalUser> userCache = CacheBuilder.newBuilder()
            .softValues()
            .expireAfterAccess(3, TimeUnit.MINUTES)
            .maximumSize((Bukkit.getMaxPlayers() * 5) / 4) // Some server owners don't appreciate the concept of 'maximum players'
            .removalListener(new RemovalListener<UUID, LocalUser>() {
                @Override
                public void onRemoval(RemovalNotification<UUID, LocalUser> notification) {
                    LocalUser user = notification.getValue();
                    for (PermissionGroup g : user.getParents()) {
                        if (g instanceof LocalGroup) {
                            ((LocalGroup) g).removeUserFromGroup(user); // No one should have references of this user here anymore.
                        }
                    }
                }
            })
            .build(new CacheLoader<UUID, LocalUser>() {
                @Override
                public LocalUser load(UUID uuid) throws Exception {
                    LocalUser user = new LocalUser(uuid, plugin, tempManager, dataSourceCache.getUnchecked(uuid), wildcardSupport);
                    user.recalculateParentData();
                    user.reloadMetadata();
                    user.reloadPermissions();
                    user.recalculatePermissions();
                    user.reloadParents(groupManager);
                    for (PermissionGroup g : user.getParents()) {
                        if (g instanceof LocalGroup) {
                            ((LocalGroup) g).addUserToGroup(user);
                        }
                    }
                    return user;
                }
            });

    private final Plugin plugin;
    private final GroupManager groupManager;
    private final UUIDHandler uuidSource;
    private final TemporaryPermissionManager tempManager;

    private final UserDataSourceFactory userDataSourceFactory;

    private final String defaultGroup;
    private final boolean wildcardSupport;

    public LocalUserManager(Plugin plugin, GroupManager groupManager, UUIDHandler uuidSource, TemporaryPermissionManager tempManager, UserDataSourceFactory userDataSourceFactory,
            String defaultGroup, boolean wildcardSupport) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(groupManager, "group manager");
        Preconditions.checkNotNull(uuidSource, "uuid source");
        Preconditions.checkNotNull(tempManager, "temp manager");
        Preconditions.checkNotNull(userDataSourceFactory, "user datasource factory");
        this.plugin = plugin;
        this.uuidSource = uuidSource;
        this.groupManager = groupManager;
        this.tempManager = tempManager;
        this.userDataSourceFactory = userDataSourceFactory;
        this.defaultGroup = defaultGroup;
        this.wildcardSupport = wildcardSupport;
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
        Preconditions.checkNotNull(player, "player");
        return getPermissionUser(player.getName());
    }

    @Override
    public LocalUser getPermissionUser(String name) {
        Preconditions.checkNotNull(name, "name");
        if (!canUserExist(name)) {
            throw new InvalidUsernameException(name);
        }
        return userCache.getUnchecked(getUniqueId(name));
    }

    @Override
    public boolean doesUserExist(OfflinePlayer player) {
        Preconditions.checkNotNull(player, "player");
        return doesUserExist(player.getName());
    }

    @Override
    public boolean doesUserExist(String name) {
        Preconditions.checkNotNull(name, "name");
        return dataSourceCache.getUnchecked(getUniqueId(name)).doesUserExist();
    }

    @Override
    public boolean canUserExist(String name) {
        Preconditions.checkNotNull(name, "name");
        if (name.length() == 0 || name.length() > 16) { // Little performance optimization
            return false;
        }
        return VALID_USERNAME_PATTERN.matcher(name).matches();
    }

    public void initializeUser(Player player) {
        Preconditions.checkNotNull(player, "player");
        LocalUser permissionUser = getPermissionUser(player);
        permissionUser.setPlayer(player);
        if (permissionUser.getParents().size() == 0) { // Set their group to the default group if possible.
            permissionUser.addParent(groupManager.getGroup(defaultGroup));
        }
    }

    public void deinitializeUser(Player player) {
        Preconditions.checkNotNull(player, "player");
        if (userCache.asMap().containsKey(player.getUniqueId())) {
            userCache.getUnchecked(player.getUniqueId()).setPlayer(null);
        }
    }
}
