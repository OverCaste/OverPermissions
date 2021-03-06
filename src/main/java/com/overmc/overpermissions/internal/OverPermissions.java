package com.overmc.overpermissions.internal;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.overmc.overpermissions.api.GroupManager;
import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.api.UserManager;
import com.overmc.overpermissions.exceptions.MissingDependencyException;
import com.overmc.overpermissions.exceptions.StartException;
import com.overmc.overpermissions.internal.commands.*;
import com.overmc.overpermissions.internal.databases.Database;
import com.overmc.overpermissions.internal.databases.mysql.MySQLManager;
import com.overmc.overpermissions.internal.datasources.UUIDHandler;
import com.overmc.overpermissions.internal.dependencies.DefaultDependencies;
import com.overmc.overpermissions.internal.dependencies.Dependency;
import com.overmc.overpermissions.internal.dependencies.DependencyDownloader;
import com.overmc.overpermissions.internal.injectoractions.BridgeInjectorAction;
import com.overmc.overpermissions.internal.injectoractions.WildcardAction;
import com.overmc.overpermissions.internal.injectoractions.WildcardDummyAction;
import com.overmc.overpermissions.internal.localentities.LocalGroupManager;
import com.overmc.overpermissions.internal.localentities.LocalUserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.security.acl.Group;
import java.util.ArrayList;
import java.util.concurrent.*;

public final class OverPermissions extends JavaPlugin {
    private TemporaryPermissionManager tempManager;
    private LocalGroupManager groupManager;
    private LocalUserManager userManager;
    private DependencyDownloader dependencyDownloader;
    private String defaultGroup;

    private Database database;
    private UUIDHandler uuidHandler;

    // Listeners
    private GeneralListener generalListener;
    private KickOnFailListener kickOnFailListener;

    // Wildcard support
    private WildcardAction wildcardAction;

    private boolean failureStarting = false;

    public final ExecutorService exec = new ThreadPoolExecutor(0, 2147483647, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactoryBuilder()
            .setNameFormat("Plugin " + getDescription().getName() + " pool thread %d").build()) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) { // This solution is taken from StackOverflow @ http://stackoverflow.com/questions/2248131/handling-exceptions-from-java-executorservice-tasks
            super.afterExecute(r, t);
            if (t == null && r instanceof Future<?>) {
                try {
                    Future<?> future = (Future<?>) r;
                    if (future.isDone()) {
                        future.get();
                    }
                } catch (CancellationException ex) {
                    t = ex;
                } catch (ExecutionException ex) {
                    t = ex.getCause();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Propegate
                }
            }
            if (t != null) {
                getLogger().severe("Uncaught exception from plugin " + getDescription().getName() + "'s pool: " + t.getMessage());
                t.printStackTrace();
            }
        }
    };

    private void initConfig( ) throws Exception {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        reloadConfig();
        defaultGroup = getConfig().getString("default-group", "default");
    }

    private void initKickOnFail( ) throws Exception {
        kickOnFailListener = new KickOnFailListener(this);
        getServer().getPluginManager().registerEvents(kickOnFailListener, this);
    }

    private void deinitKickOnFail( ) throws Exception {
        HandlerList.unregisterAll(kickOnFailListener);
    }

    private void initDependencies( ) throws Exception {
        dependencyDownloader = new DependencyDownloader(getDataFolder().getParentFile());
        ArrayList<Dependency> missingDependencies = new ArrayList<>(5);
        if (getConfig().getBoolean("sql.use-pool", false)) {
            dependencyDownloader.ensureDependencyExists(DefaultDependencies.HIKARI_CP, missingDependencies);
        }
        if (!missingDependencies.isEmpty()) {
            throw new MissingDependencyException(missingDependencies);
        }
    }

    private void initManagers( ) throws Exception {
        String type = getConfig().getString("sql.type", "mysql").toLowerCase();
        String wildcardSupportValue = getConfig().getString("wildcard-support", "STANDARD");
        boolean wildcardSupport;
        if (wildcardSupportValue.equals("STANDARD")) {
            wildcardSupport = true;
        } else if (wildcardSupportValue.equals("NONE")) {
            wildcardSupport = false;
        } else {
            throw new StartException("The configuration option wildcard-support is set to an invalid value: " + wildcardSupportValue);
        }
        Database database;
        switch (type) {
            default:
                getLogger().warning("Type value " + type + " wasn't recognized. Defaulting to mysql.");
            case "mysql": {
                database = new MySQLManager(getLogger(), exec, defaultGroup,
                        getConfig().getString("sql.address", "localhost"),
                        getConfig().getString("sql.port", ""),
                        getConfig().getString("sql.dbname", "OverPermissions"),
                        getConfig().getString("sql.dbusername", "root"),
                        getConfig().getString("sql.dbpassword", ""),
                        getConfig().getBoolean("sql.use-pool", false),
                        getConfig().getBoolean("force-online-mode", false));
            }

        }
        this.database = database;
        uuidHandler = database.getUUIDHandler();
        tempManager = new TemporaryPermissionManager(this, database);
        groupManager = new LocalGroupManager(database, tempManager, wildcardSupport);
        groupManager.reloadGroups();
        userManager = new LocalUserManager(this, groupManager, uuidHandler, tempManager, database, getDefaultGroupName(), wildcardSupport);
    }

    private void initCommands( ) throws Exception {
        new GroupCreateCommand(this).register();
        new GroupDeleteCommand(this).register();
        new GroupAddCommand(this).register();
        new GroupAddTempCommand(this).register();
        new GroupRemoveCommand(this).register();
        new GroupRemoveTempCommand(this).register();
        new GroupAddParentCommand(this).register();
        new GroupRemoveParentCommand(this).register();
        new GroupSetMetaCommand(this).register();

        new PlayerSetGroupCommand(this).register();
        new PlayerPromoteCommand(this).register();
        new PlayerAddGroupCommand(this).register();
        new PlayerRemoveGroupCommand(this).register();
        new PlayerAddTempCommand(this).register();
        new PlayerRemoveTempCommand(this).register();
        new PlayerSetMetaCommand(this).register();
        new PlayerAddCommand(this).register();
        new PlayerRemoveCommand(this).register();
        new PlayerCheckCommand(this).register();

        new OverPermissionsCommand(this).register();
    }

    private void injectBukkitActions( ) {
        String injectionModeValue = getConfig().getString("injection-mode", "FULL");
        if (injectionModeValue.equalsIgnoreCase("FULL")) {
            wildcardAction = new BridgeInjectorAction(userManager);
        } else if (injectionModeValue.equalsIgnoreCase("NONE")) {
            wildcardAction = new WildcardDummyAction();
        } else {
            throw new StartException("Invalid configuration option: 'injection-mode': (" + injectionModeValue + ")");
        }
    }

    private void registerEvents( ) {
        generalListener = new GeneralListener(this);
        getServer().getPluginManager().registerEvents(generalListener, this);
    }

    private void registerApi( ) {
        getServer().getServicesManager().register(UserManager.class, userManager, this, ServicePriority.Normal);
        getServer().getServicesManager().register(GroupManager.class, groupManager, this, ServicePriority.Normal);
        Bukkit.getServicesManager().getRegistration(UserManager.class).getProvider();
    }

    void initPlayer(Player player) {
        wildcardAction.initializePlayer(player);
        userManager.initializeUser(player);
        PermissionUser user = userManager.getPermissionUser(player);
        tempManager.initializePlayerTemporaryPermissions(user);
    }

    void initPlayerUUID(Player player) {
        uuidHandler.setNameUuid(player.getName(), player.getUniqueId());
    }

    private void initPlayers( ) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            initPlayerUUID(p);
            initPlayer(p);
        }
    }

    @Override
    public void onEnable( ) {
        try {
            initConfig();
            initKickOnFail();
            initDependencies();
            initManagers();
            initCommands();
            injectBukkitActions();
            registerEvents();
            registerApi();
            initPlayers();
            deinitKickOnFail(); // Started successfully, can remove this listener.
            getLogger().info("Successfully enabled.");
        } catch (MissingDependencyException e) {
            Bukkit.getLogger().warning(e.getSimpleMessage());
            Bukkit.getServer().shutdown();
        } catch (StartException e) {
            failureStarting = true;
            Bukkit.getLogger().severe("OverPermissions failed to start: " + e.getSimpleMessage());
        } catch (Exception e) {
            e.printStackTrace();
            failureStarting = true;
        }
    }

    void deinitPlayer(Player player) {
        tempManager.cancelTemporaryPermissions(userManager.getPermissionUser(player));
        wildcardAction.deinitializePlayer(player);
    }

    private void deinitPlayers( ) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            deinitPlayer(p);
        }
    }

    @Override
    public void onDisable( ) {
        if (!failureStarting) {
            deinitPlayers();
            getLogger().info("disabled.");
        }
        if (exec != null) {
            exec.shutdown();
        }
        if (database != null) {
            try {
                database.shutdown();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // API methods

    /**
     * @return the default {@link Group}'s name, specified in the configuration.
     */
    public String getDefaultGroupName( ) {
        return defaultGroup;
    }

    /**
     * @return the Database connector that this OverPermissions instance is using to store persistant data.
     */
    public Database getPermissionDatabase( ) {
        return database;
    }

    /**
     * @return the retriever/handler for player name <-> UUID relation ships
     */
    public UUIDHandler getUniqueIdHandler( ) {
        return uuidHandler;
    }

    /**
     * @return the {@link ExecutorService} used by this plugin, with proper error recording.
     */
    public ExecutorService getExecutor( ) {
        return exec;
    }

    public GroupManager getGroupManager( ) {
        return groupManager;
    }

    public UserManager getUserManager( ) {
        return userManager;
    }
}
