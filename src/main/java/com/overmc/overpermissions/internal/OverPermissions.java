package com.overmc.overpermissions.internal;

import java.io.IOException;
import java.security.acl.Group;
import java.util.concurrent.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.overmc.overpermissions.api.*;
import com.overmc.overpermissions.internal.commands.*;
import com.overmc.overpermissions.internal.databases.DatabaseMultiSourceFactory;
import com.overmc.overpermissions.internal.databases.mysql.MySQLManager;
import com.overmc.overpermissions.internal.datasources.*;
import com.overmc.overpermissions.internal.injectoractions.*;
import com.overmc.overpermissions.internal.localentities.LocalGroupManager;
import com.overmc.overpermissions.internal.localentities.LocalUserManager;
import com.overmc.overpermissions.internal.metrics.MetricsLite;

public final class OverPermissions extends JavaPlugin {
    private TemporaryPermissionManager tempManager;
    private LocalGroupManager groupManager;
    private LocalUserManager userManager;
    private MetricsLite metrics;
    private String defaultGroup;

    private GroupManagerDataSourceFactory groupDataSourceFactory;
    private UserDataSourceFactory userDataSourceFactory;
    private UUIDDataSource uuidDataSource;

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

    private void initConfig( ) throws Throwable {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        reloadConfig();
    }

    private void initKickOnFail( ) throws Throwable {
        kickOnFailListener = new KickOnFailListener(this);
        getServer().getPluginManager().registerEvents(kickOnFailListener, this);
    }

    private void deinitKickOnFail( ) throws Throwable {
        HandlerList.unregisterAll(kickOnFailListener);
    }

    private void initManagers( ) throws Throwable {
        String type = getConfig().getString("sql.type", "mysql").toLowerCase();
        String wildcardSupportValue = getConfig().getString("wildcard-support", "STANDARD");
        boolean wildcardSupport;
        if(wildcardSupportValue.equals("STANDARD")) {
            wildcardSupport = true;
        } else if(wildcardSupportValue.equals("NONE")){
            wildcardSupport = false;
        } else {
            throw new StartException("The configuration option wildcard-support is set to an invalid value: " + wildcardSupportValue);
        }
        DatabaseMultiSourceFactory database;
        switch (type) {
            default:
                getLogger().warning("Type value " + type + " wasn't recognized. Defaulting to mysql.");
            case "mysql": {
                database = new MySQLManager(exec,
                        "jdbc:mysql://" + getConfig().getString("sql.address", "localhost") + "/",
                        getConfig().getString("sql.dbname", "OverPermissions"),
                        getConfig().getString("sql.dbusername", "root"),
                        getConfig().getString("sql.dbpassword", ""));
            }

        }
        groupDataSourceFactory = database;
        uuidDataSource = database.createUUIDDataSource();
        userDataSourceFactory = database;
        tempManager = new TemporaryPermissionManager(this, database);
        groupManager = new LocalGroupManager(groupDataSourceFactory, tempManager, wildcardSupport);
        groupManager.reloadGroups();
        userManager = new LocalUserManager(this, groupManager, uuidDataSource, tempManager, userDataSourceFactory, getDefaultGroupName(), wildcardSupport);
    }

    private void initDefaultGroup( ) {
        defaultGroup = getConfig().getString("default-group", "default");
        if (!groupManager.doesGroupExist(defaultGroup)) { // group doesn't exist.
            groupManager.createGroup(defaultGroup, 0);
            getLogger().info("Successfully created default group: " + defaultGroup);
        }
    }

    private void initCommands( ) throws Throwable {
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
        String wildcardSupportValue = getConfig().getString("wildcard-support", "HALF");
        String injectionModeValue = getConfig().getString("injection-mode", "FULL");
        if(injectionModeValue.equalsIgnoreCase("FULL")) {
            if(wildcardSupportValue.equals("STANDARD")) {
                wildcardAction = new HalfWildcardPlayerInjectorAction(this);
            } else if(wildcardSupportValue.equals("NONE")) {
                wildcardAction = new WildcardDummyAction();
            } else {
                throw new StartException("Invalid configuration option: 'wildcard-support': (" + wildcardSupportValue + ")");
            }
        } else if(injectionModeValue.equalsIgnoreCase("NONE")){
            //The wildcard support case is handled in 'initManagers( )' for now.
        } else {
            throw new StartException("Invalid configuration option: 'injection-mode': (" + injectionModeValue + ")");
        }
    }

    private void registerEvents( ) {
        generalListener = new GeneralListener(this);
        getServer().getPluginManager().registerEvents(generalListener, this);
    }

    private void initMetrics( ) {
        try {
            metrics = new MetricsLite(this);
            metrics.start();
            getLogger().info("Successfully connected to metrics!");
        } catch (IOException e) {
            getLogger().warning("Failed to connect to the metrics server!");
            e.printStackTrace();
        }
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

    private void initPlayers( ) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            initPlayer(p);
        }
    }

    @Override
    public void onEnable( ) {
        try {
            initConfig();
            initKickOnFail();
            initManagers();
            initDefaultGroup();
            initCommands();
            injectBukkitActions();
            registerEvents();
            initMetrics();
            registerApi();
            initPlayers();
            deinitKickOnFail(); // Started successfully, can remove this listener.
            getLogger().info(ChatColor.GREEN + "Successfully enabled!");
        } catch (StartException e) {
            failureStarting = true;
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "OverPermissions failed to start: " + e.getSimpleMessage());
        } catch (Throwable t) {
            t.printStackTrace();
            failureStarting = true;
        }
    }

    void deinitPlayer(Player player) {
        tempManager.cancelTemporaryPermissions(userManager.getPermissionUser(player));
        userManager.deinitializeUser(player);
        wildcardAction.deinitializePlayer(player);
    }

    private void deinitPlayers( ) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            deinitPlayer(p);
        }
    }

    @Override
    public void onDisable( ) {
        exec.shutdown();
        if (!failureStarting) {
            deinitPlayers();
            getLogger().info("disabled.");
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
