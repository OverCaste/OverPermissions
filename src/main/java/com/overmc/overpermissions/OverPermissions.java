package com.overmc.overpermissions;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.overmc.overpermissions.commands.GroupAddCommand;
import com.overmc.overpermissions.commands.GroupAddParentCommand;
import com.overmc.overpermissions.commands.GroupAddTempCommand;
import com.overmc.overpermissions.commands.GroupCreateCommand;
import com.overmc.overpermissions.commands.GroupDeleteCommand;
import com.overmc.overpermissions.commands.GroupRemoveCommand;
import com.overmc.overpermissions.commands.GroupRemoveParentCommand;
import com.overmc.overpermissions.commands.GroupSetMetaCommand;
import com.overmc.overpermissions.commands.OverPermissionsCommand;
import com.overmc.overpermissions.commands.PlayerAddCommand;
import com.overmc.overpermissions.commands.PlayerAddGroupCommand;
import com.overmc.overpermissions.commands.PlayerAddTempCommand;
import com.overmc.overpermissions.commands.PlayerCheckCommand;
import com.overmc.overpermissions.commands.PlayerPromoteCommand;
import com.overmc.overpermissions.commands.PlayerRemoveCommand;
import com.overmc.overpermissions.commands.PlayerSetGroupCommand;
import com.overmc.overpermissions.commands.PlayerSetMetaCommand;
import com.overmc.overpermissions.metrics.MetricsLite;

public class OverPermissions extends JavaPlugin {
	public static final int GLOBAL_WORLD_ID = 1;
	private static HashMap<Player, PlayerPermissionData> players = new HashMap<Player, PlayerPermissionData>();
	private static HashMap<Player, Future<PlayerPermissionData>> playerFutures = new HashMap<Player, Future<PlayerPermissionData>>();
	public static final ExecutorService exec = Executors.newCachedThreadPool();

	private SQLManager sqlManager;
	private GroupManager groupManager;
	private TimedPermissionManager tempManager;
	private OverPermissionsAPI permissionsAPI;
	private MetricsLite metrics;
	private String defaultGroup;
	private int defaultGroupId;

	private boolean failureStarting = false;

	@Override
	public void onEnable( ) {
		try {
			initConfig();
			initKickOnFail();
			initManagers();
			initCommands();
			registerEvents();
			initAPI();
			initMetrics();
			initPlayers();
			getLogger().info(ChatColor.GREEN + "Successfully enabled!");
		} catch (StartException e) {
			this.failureStarting = true;
			getLogger().severe(ChatColor.RED + "Failed to start: " + e.getSimpleMessage());
		} catch (Throwable t) {
			t.printStackTrace();
			this.failureStarting = true;
		}
	}

	@Override
	public void onDisable( ) {
		getLogger().info("disabled.");
		deinitPlayers();
	}

	private void deinitPlayers( ) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			deinitPlayer(p);
		}
	}

	private void initConfig( ) throws Throwable {
		saveDefaultConfig();
		getConfig().options().copyDefaults(true);
		saveConfig();
		reloadConfig();
	}

	private void initKickOnFail( ) throws Throwable {
		getServer().getPluginManager().registerEvents(new KickOnFailListener(this), this);
	}

	private void initManagers( ) throws Throwable {
		String type = getConfig().getString("sql.type", "sqlite");
		if (type.equalsIgnoreCase("sqlite")) {
			// TODO sqlManager = new SQLiteManager(this)
		} else if (type.equalsIgnoreCase("mysql")) {
			this.sqlManager = new MySQLManager(this,
					"jdbc:mysql://" + getConfig().getString("sql.address", "localhost") + "/",
					getConfig().getString("sql.dbname", "OverPermissions"),
					getConfig().getString("sql.dbusername", "root"),
					getConfig().getString("sql.dbpassword", ""));
		} else {
			getLogger().warning("Type value " + type + " wasn't recognized. Defaulting to sqlite.");
		}
		initDefaultGroup();
		this.tempManager = new TimedPermissionManager(this);
		this.groupManager = new GroupManager(this);
		this.groupManager.recalculateGroups();
	}

	private void initDefaultGroup( ) {
		this.defaultGroup = getConfig().getString("default-group", "default");
		if (this.sqlManager.getGroupId(this.defaultGroup) < 0) { // group doesn't exist.
			this.sqlManager.createGroup(this.defaultGroup, GLOBAL_WORLD_ID);
			getLogger().info("Successfully created default group: " + this.defaultGroup);
		}
		this.defaultGroupId = this.sqlManager.getGroupId(this.defaultGroup);
		// sqlManager.setGroup(defaultGroup, sqlManager.getGroupPriority(defaultGroupId), -1); // Force default group to global, things would seriously mess up otherwise. (groups don't have worlds anymore.)
	}

	private void initCommands( ) throws Throwable {
		new GroupCreateCommand(this).register();
		new GroupDeleteCommand(this).register();
		new GroupSetMetaCommand(this).register();
		new GroupAddCommand(this).register();
		new GroupAddTempCommand(this).register();
		new GroupAddParentCommand(this).register();
		new GroupRemoveParentCommand(this).register();
		new GroupRemoveCommand(this).register();

		new PlayerSetGroupCommand(this).register();
		new PlayerAddGroupCommand(this).register();
		new PlayerPromoteCommand(this).register();
		new PlayerSetMetaCommand(this).register();
		new PlayerAddCommand(this).register();
		new PlayerAddTempCommand(this).register();
		new PlayerRemoveCommand(this).register();
		new PlayerCheckCommand(this).register();

		new OverPermissionsCommand(this).register();
	}

	private void registerEvents( ) {
		getServer().getPluginManager().registerEvents(new GeneralListener(this), this);
	}

	private void initAPI( ) {
		this.permissionsAPI = new OverPermissionsAPI(this);
	}

	private void initMetrics( ) {
		try {
			this.metrics = new MetricsLite(this);
			this.metrics.start();
			getLogger().info("Successfully connected to metrics!");
		} catch (IOException e) {
			getLogger().warning("Failed to connect to the metrics server!");
			e.printStackTrace();
		}
	}

	private void initPlayers( ) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			initPlayer(p);
		}
	}

	/**
	 * Only use this if you know what you're doing, the API layer is much safer.
	 * 
	 * @see #getAPI()
	 */
	public SQLManager getSQLManager( ) {
		return this.sqlManager;
	}

	/**
	 * Only use this if you know what you're doing, the API layer is much safer.
	 * 
	 * @see #getAPI()
	 */
	public GroupManager getGroupManager( ) {
		return this.groupManager;
	}

	/**
	 * Only use this if you know what you're doing, the API layer is much safer.
	 * 
	 * @see #getAPI()
	 */
	public TimedPermissionManager getTempManager( ) {
		return this.tempManager;
	}

	/**
	 * Only use this if you know what you're doing, the API layer is much safer.
	 * 
	 * @see #getAPI()
	 */
	public PlayerPermissionData getPlayerPermissions(Player player) {
		try {
			PlayerPermissionData d = players.get(player);
			if (d == null) {
				initPlayer(player);
				Future<PlayerPermissionData> future = playerFutures.remove(player);
				PlayerPermissionData playerData = future.get();
				players.put(player, playerData);
				return playerData;
			}
			return d;
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	protected void initPlayer(Player player) {
		if (players.containsKey(player) || playerFutures.containsKey(player)) {
			return;
		}
		this.tempManager.init(player);
		playerFutures.put(player, getPlayerFuture(player));
	}

	protected Future<PlayerPermissionData> getPlayerFuture(final Player player) {
		return exec.submit(new Callable<PlayerPermissionData>() {
			@Override
			public PlayerPermissionData call( ) throws Exception {
				PlayerPermissionData playerData = new PlayerPermissionData(OverPermissions.this, OverPermissions.this.sqlManager.getPlayerId(player.getName(), true), OverPermissions.this.sqlManager
						.getWorldId(player.getWorld().getName(), true),
						player);
				playerData.recalculateGroups();
				playerData.recalculatePermissions();
				playerData.recalculateMeta();
				return playerData;
			}
		});
	}

	protected void deinitPlayer(Player player) {
		this.tempManager.deinit(player);
		if (players.containsKey(player)) {
			players.get(player).unset();
			players.remove(player);
		}
		playerFutures.remove(player);
	}

	// API
	public OverPermissionsAPI getAPI( ) {
		return this.permissionsAPI;
	}

	public boolean checkFailure( ) {
		return this.failureStarting;
	}

	public int getDefaultGroupId( ) {
		return this.defaultGroupId;
	}

	public String getDefaultGroup( ) {
		return this.defaultGroup;
	}

	public ExecutorService getExecutor( ) {
		return exec;
	}
}
