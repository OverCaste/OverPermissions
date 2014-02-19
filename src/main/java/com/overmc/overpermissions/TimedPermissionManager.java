package com.overmc.overpermissions;

import java.util.*;
import java.util.concurrent.*;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.*;

import com.google.common.collect.Maps;

public class TimedPermissionManager {
	private final OverPermissions plugin;
	private final BukkitScheduler scheduler;

	private final HashMap<TimedPlayerPermission, BukkitTask> playerTempPermissionMap = Maps.newHashMap();
	private final HashMap<TimedGroupPermission, BukkitTask> groupTempPermissionMap = Maps.newHashMap();
	private final HashMap<Player, ArrayList<TimedPlayerPermission>> playerLookupMap = Maps.newHashMap();
	private final HashMap<Player, Future<ArrayList<TimedPlayerPermission>>> playerFutureMap = Maps.newHashMap();
	private final HashMap<Group, ArrayList<TimedGroupPermission>> groupLookupMap = Maps.newHashMap();

	public TimedPermissionManager(OverPermissions plugin) {
		this.plugin = plugin;
		scheduler = plugin.getServer().getScheduler();
	}

	public boolean registerTemporaryPlayerPermission(final int playerId, final int worldId, int timeInSeconds, final String node) {
		boolean success = false;
		long executeTime = (System.currentTimeMillis() / 1000) + timeInSeconds;
		if (worldId < 0) {
			success = plugin.getSQLManager().addGlobalPlayerPermissionTimeout(playerId, node, executeTime);
		} else {
			success = plugin.getSQLManager().addPlayerPermissionTimeout(playerId, worldId, node, executeTime);
		}
		if (success) {
			final TimedPlayerPermission perm = new TimedPlayerPermission(worldId, playerId, node, executeTime);
			BukkitTask oldTask = playerTempPermissionMap.remove(perm);
			if (oldTask != null) {
				oldTask.cancel();
			}
			playerTempPermissionMap.put(perm, scheduler.runTaskLater(plugin, new Runnable() {
				@Override
				public void run( ) {
					if (worldId < 0) {
						plugin.getSQLManager().removeGlobalPlayerPermissionTimeout(playerId, node);
					} else {
						plugin.getSQLManager().removePlayerPermissionTimeout(playerId, worldId, node);
					}
					playerTempPermissionMap.remove(perm);
					Player p = plugin.getSQLManager().getPlayer(playerId);
					if (p != null) {
						getPlayerTempPermissions(p).remove(perm);
						plugin.getPlayerPermissions(p).recalculatePermissions();
					}
				}
			}, timeInSeconds * 20));
			Player p = plugin.getSQLManager().getPlayer(playerId);
			if (p != null) {
				getPlayerTempPermissions(p).add(perm);
				plugin.getPlayerPermissions(p).recalculatePermissions();
			}
			return true;
		}
		return false;
	}

	public boolean registerTemporaryGroupPermission(final int groupId, final int timeInSeconds, final String node) {
		long executeTime = (System.currentTimeMillis() / 1000) + timeInSeconds;
		if (plugin.getSQLManager().addGroupPermissionTimeout(groupId, node, executeTime)) {
			final TimedGroupPermission tempPerm = new TimedGroupPermission(groupId, node, executeTime);
			BukkitTask oldTask = groupTempPermissionMap.remove(tempPerm);
			if (oldTask != null) {
				oldTask.cancel();
			}
			groupTempPermissionMap.put(tempPerm, scheduler.runTaskLater(plugin, new Runnable() {
				@Override
				public void run( ) {
					plugin.getSQLManager().removeGroupPermissionTimeout(groupId, node);
					groupTempPermissionMap.remove(tempPerm);
					Group g = plugin.getGroupManager().getGroup(groupId);
					if (g != null) {
						groupLookupMap.get(g).remove(tempPerm);
						g.recalculatePermissions();
					}
				}
			}, timeInSeconds * 20));
			Group g = plugin.getGroupManager().getGroup(groupId);
			if (g != null) {
				groupLookupMap.get(g).add(tempPerm);
				g.recalculatePermissions();
			}
			return true;
		}
		return false;
	}

	public boolean cancelTemporaryPlayerPermission(int playerId, int worldId, String node) {
		TimedPlayerPermission tempPerm = new TimedPlayerPermission(worldId, playerId, node, 0L);
		BukkitTask task = playerTempPermissionMap.remove(tempPerm);
		if (task != null) {
			task.cancel();
			plugin.getSQLManager().removePlayerPermissionTimeout(playerId, worldId, node);
			Player p = plugin.getSQLManager().getPlayer(playerId);
			if (p != null) {
				getPlayerTempPermissions(p).remove(tempPerm);
				plugin.getPlayerPermissions(p).recalculatePermissions();
			}
			return true;
		}
		return false;
	}

	public boolean cancelTemporaryGroupPermission(int groupId, String node) {
		TimedGroupPermission tempPerm = new TimedGroupPermission(groupId, node, 0L);
		BukkitTask task = playerTempPermissionMap.remove(tempPerm);
		if (task != null) {
			task.cancel();
			plugin.getSQLManager().removeGroupPermissionTimeout(groupId, node);
			Group g = plugin.getGroupManager().getGroup(groupId);
			if (g != null) {
				groupLookupMap.get(g).remove(tempPerm);
				g.recalculatePermissions();
			}
			return true;
		}
		return false;
	}

	public void init(final Player p) {
		playerFutureMap.put(p, OverPermissions.exec.submit(new Callable<ArrayList<TimedPlayerPermission>>() {
			@Override
			public ArrayList<TimedPlayerPermission> call( ) throws Exception {
				return loadPlayerTempPermissions(p);
			}
		}));
	}

	private ArrayList<TimedPlayerPermission> loadPlayerTempPermissions(final Player p) {
		ArrayList<TimedPlayerPermission> playerTimedPermissions = new ArrayList<TimedPlayerPermission>();
		final int playerId = plugin.getSQLManager().getPlayerId(p.getName(), false);
		if (playerId < 0) {
			return playerTimedPermissions;
		}
		for (final TimedPlayerPermission tempPermission : plugin.getSQLManager().getPlayerPermissionTimeouts(playerId)) {
			int timeInSeconds = (int) ((System.currentTimeMillis() / 1000) - tempPermission.executeTime);
			playerTimedPermissions.add(tempPermission);
			playerTempPermissionMap.put(tempPermission, scheduler.runTaskLater(plugin, new Runnable() {
				@Override
				public void run( ) {
					if (tempPermission.worldId < 0) {
						plugin.getSQLManager().removeGlobalPlayerPermissionTimeout(playerId, tempPermission.node);
					} else {
						plugin.getSQLManager().removePlayerPermissionTimeout(playerId, tempPermission.worldId, tempPermission.node);
					}
					Player p = plugin.getSQLManager().getPlayer(playerId);
					if (p != null) {
						plugin.getPlayerPermissions(p).recalculatePermissions();
					}
				}
			}, timeInSeconds * 20));
		}
		return playerTimedPermissions;
	}

	private ArrayList<TimedPlayerPermission> getPlayerTempPermissions(Player p) {
		ArrayList<TimedPlayerPermission> ret = playerLookupMap.get(p);
		try {
			if (ret == null) {
				Future<ArrayList<TimedPlayerPermission>> future = playerFutureMap.get(p);
				if (future == null) {
					loadPlayerTempPermissions(p);
					future = playerFutureMap.get(p);
				}
				ret = future.get();
				playerFutureMap.remove(p);
				playerLookupMap.put(p, ret);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return ret;
	}

	public void deinit(Player p) {
		ArrayList<TimedPlayerPermission> playerTempPermissions = playerLookupMap.remove(p);
		if (playerTempPermissions == null) {
			try {
				playerTempPermissions = playerFutureMap.remove(p).get();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		if (playerTempPermissions != null) {
			for (TimedPlayerPermission perm : playerTempPermissions) {
				BukkitTask task = playerTempPermissionMap.remove(perm);
				if (task != null) {
					task.cancel();
				}
			}
		}
	}

	public void init(Group g) {
		if (g == null) {
			return;
		}
		final int groupId = g.getId();
		ArrayList<TimedGroupPermission> groupTimedPermissions = new ArrayList<TimedGroupPermission>();
		for (final TimedGroupPermission tempPermission : plugin.getSQLManager().getGroupPermissionTimeouts(groupId)) {
			int timeInSeconds = (int) ((System.currentTimeMillis() / 1000) - tempPermission.executeTime);
			groupTimedPermissions.add(tempPermission);
			groupTempPermissionMap.put(tempPermission, scheduler.runTaskLater(plugin, new Runnable() {
				@Override
				public void run( ) {
					plugin.getSQLManager().removeGroupPermissionTimeout(groupId, tempPermission.node);
					Group g = plugin.getGroupManager().getGroup(groupId);
					if (g != null) {
						g.recalculatePermissions();
					}
				}
			}, timeInSeconds * 20));
		}
		groupLookupMap.put(g, groupTimedPermissions);
	}

	public void deinit(Group g) {
		ArrayList<TimedGroupPermission> groupTempPermissions = groupLookupMap.remove(g);
		if (groupTempPermissions != null) {
			for (TimedGroupPermission perm : groupTempPermissions) {
				BukkitTask task = groupTempPermissionMap.remove(perm);
				if (task != null) {
					task.cancel();
				}
			}
		}
	}

	public List<String> getPlayerNodes(Player p) {
		ArrayList<String> nodes = new ArrayList<String>();
		ArrayList<TimedPlayerPermission> playerTempPermissions = getPlayerTempPermissions(p);
		if (playerTempPermissions == null) {
			return nodes;
		}
		for (TimedPlayerPermission t : playerTempPermissions) {
			nodes.add(t.node);
		}
		return nodes;
	}

	public List<String> getGroupNodes(Group g) {
		ArrayList<String> nodes = new ArrayList<String>();
		ArrayList<TimedGroupPermission> playerTempPermissions = groupLookupMap.get(g);
		for (TimedGroupPermission t : playerTempPermissions) {
			nodes.add(t.node);
		}
		return nodes;
	}
}
