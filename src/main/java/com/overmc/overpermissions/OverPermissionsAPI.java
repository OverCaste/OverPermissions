package com.overmc.overpermissions;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.collect.Iterables;
import com.overmc.overpermissions.events.PlayerPermissionAddEvent;
import com.overmc.overpermissions.events.PlayerPermissionRemoveEvent;

public class OverPermissionsAPI {
	private final OverPermissions plugin;

	protected OverPermissionsAPI(OverPermissions plugin) {
		this.plugin = plugin;
	}

	public boolean playerHas(String worldName, String playerName, String permission)
	{
		Player p = plugin.getServer().getPlayerExact(playerName);
		if (p == null) {
			int playerId = plugin.getSQLManager().getPlayerId(playerName);
			int worldId = plugin.getSQLManager().getWorldId(worldName);
			return plugin.getSQLManager().checkPlayerPermission(playerId, worldId, permission);
		} else {
			return p.hasPermission(permission);
		}
	}

	public boolean playerAdd(String worldName, String playerName, String permission)
	{
		PlayerPermissionAddEvent e = new PlayerPermissionAddEvent(playerName, worldName, permission, PermissionChangeCause.API);
		plugin.getServer().getPluginManager().callEvent(e);
		if (e.isCancelled()) {
			return false;
		}
		Player p = plugin.getServer().getPlayerExact(playerName);
		int playerId = (p == null) ? plugin.getUuidManager().getOrCreateSqlUser(playerName) : plugin.getPlayerPermissions(p).getId();
        if(playerId < 0) {
            return false; //Player uid doesn't exist
        }
		boolean value;
		if ((worldName == null) || (worldName.length() == 0)) {
			value = plugin.getSQLManager().addGlobalPlayerPermission(playerId, permission);
		} else {
			int worldId = plugin.getSQLManager().getWorldId(worldName, false);
			if (worldId < 0) {
				return false;
			}
			value = plugin.getSQLManager().addPlayerPermission(playerId, worldId, permission);
		}
		if (value && (p != null)) {
			plugin.getPlayerPermissions(p).recalculatePermissions();
		}
		return value;
	}

	public boolean playerRemove(String worldName, String playerName, String permission)
	{
		PlayerPermissionRemoveEvent e = new PlayerPermissionRemoveEvent(playerName, worldName, permission, PermissionChangeCause.API);
		plugin.getServer().getPluginManager().callEvent(e);
		if (e.isCancelled()) {
			return false;
		}
		Player p = plugin.getServer().getPlayerExact(playerName);
		int playerId;
		// = (p == null) ? plugin.getSQLManager().getPlayerId(playerName, true) : plugin.getPlayerPermissions(p).getId();
		if(p == null) {
             playerId = plugin.getUuidManager().getOrCreateSqlUser(playerName);
            if(playerId < 0) {
                return false;
            }
		} else {
		    playerId = plugin.getPlayerPermissions(p).getId();
		}
		boolean value;
		if ((worldName == null) || (worldName.length() == 0)) {
			value = plugin.getSQLManager().removeGlobalPlayerPermission(playerId, permission);
		} else {
			int worldId = plugin.getSQLManager().getWorldId(worldName, false);
			if (worldId < 0) {
				return false;
			}
			value = plugin.getSQLManager().removePlayerPermission(playerId, worldId, permission);
		}
		if (value && (p != null)) {
			plugin.getPlayerPermissions(p).recalculatePermissions();
		}
		return value;
	}

	public boolean groupHas(String groupName, String permission)
	{
		Group group = plugin.getGroupManager().getGroup(groupName);
		if (group == null) {
			return false;
		}
		return group.hasPermission(permission);
	}

	public boolean groupAdd(String groupName, String permission)
	{
		Group group = plugin.getGroupManager().getGroup(groupName);
		if (group == null) {
			return false;
		}
		boolean value = plugin.getSQLManager().addGroupPermission(group.getId(), permission);
		group.recalculatePermissions();
		return value;
	}

	public boolean groupRemove(String groupName, String permission)
	{
		Group group = plugin.getGroupManager().getGroup(groupName);
		if (group == null) {
			return false;
		}
		boolean value = plugin.getSQLManager().removeGroupPermission(group.getId(), permission);
		group.recalculatePermissions();
		return value;
	}

	public boolean groupHasPlayer(String playerName, String groupName)
	{
		Player p = plugin.getServer().getPlayerExact(playerName);
		Group group = plugin.getGroupManager().getGroup(groupName);
		if (group == null) {
			return false;
		}
		if (p != null) {
			return plugin.getPlayerPermissions(p).isInGroup(group);
		} else {
			int playerId = plugin.getSQLManager().getPlayerId(playerName);
			return plugin.getSQLManager().isPlayerInGroup(playerId, group.getId());
		}
	}

	public boolean playerAddGroup(String playerName, String groupName)
	{
		Player p = plugin.getServer().getPlayerExact(playerName);
		Group group = plugin.getGroupManager().getGroup(groupName);
		if (group == null) {
			return false;
		}
		int playerId = plugin.getSQLManager().getPlayerId(playerName);
		boolean value = plugin.getSQLManager().addPlayerGroup(playerId, group.getId());
		if (p != null) {
			plugin.getPlayerPermissions(p).recalculatePermissions();
		}
		return value;
	}

	public boolean playerRemoveGroup(String playerName, String groupName)
	{
		Player p = plugin.getServer().getPlayerExact(playerName);
		Group group = plugin.getGroupManager().getGroup(groupName);
		if (group == null) {
			return false;
		}
		int playerId = plugin.getSQLManager().getPlayerId(playerName);
		boolean success = plugin.getSQLManager().removePlayerGroup(playerId, group.getId());
		if (p != null) {
			plugin.getPlayerPermissions(p).recalculatePermissions();
		}
		return success;
	}

	public String[] getPlayerGroups(String worldName, String playerName)
	{
		Player p = plugin.getServer().getPlayerExact(playerName);
		ArrayList<String> ret = new ArrayList<String>();
		if (p == null) {
			int playerId = plugin.getSQLManager().getPlayerId(playerName);
			for (Integer groupId : plugin.getSQLManager().getPlayerGroups(playerId)) {
				Group group = plugin.getGroupManager().getGroup(groupId);
				if ((group.getWorld() == null) || group.getWorld().getName().equalsIgnoreCase(worldName)) {
					ret.add(group.getName());
				}
			}
		} else {
			for (Group g : plugin.getPlayerPermissions(p).getEffectiveGroups()) {
				ret.add(g.getName());
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	public String getPrimaryGroup(String worldName, String playerName)
	{
		String[] playerGroups = getPlayerGroups(worldName, playerName);
		if (playerGroups.length == 0) {
			return null;
		}
		return playerGroups[0];
	}

	public boolean playerAddTransient(String world, String player, String permission)
	{
		if (world != null) {
			throw new UnsupportedOperationException("OverPermissions does not support World based transient permissions!");
		}
		Player p = plugin.getServer().getPlayerExact(player);
		if (p == null) {
			throw new UnsupportedOperationException("OverPermissions does not support offline player transient permissions!");
		}

		plugin.getPlayerPermissions(p).setTransient(permission, true);

		return true;
	}

	public boolean playerRemoveTransient(String worldName, String playerName, String permission)
	{
		if (worldName != null) {
			throw new UnsupportedOperationException("OverPermissions does not support World based transient permissions!");
		}
		Player p = plugin.getServer().getPlayerExact(playerName);
		if (p == null) {
			throw new UnsupportedOperationException("OverPermissions does not support offline player transient permissions!");
		}

		return plugin.getPlayerPermissions(p).clearTransient(permission);
	}

	public boolean playerAddTemporary(String worldName, String playerName, String permission, int timeInSeconds) {
		int playerId = plugin.getSQLManager().getPlayerId(playerName);
		if (playerId < 0) {
			return false;
		}
		int worldId = plugin.getSQLManager().getWorldId(worldName);
		return plugin.getTempManager().registerTemporaryPlayerPermission(playerId, worldId, timeInSeconds, permission);
	}

	public boolean playerRemoveTemporary(String worldName, String playerName, String permission) {
		int playerId = plugin.getSQLManager().getPlayerId(playerName);
		if (playerId < 0) {
			return false;
		}
		int worldId = plugin.getSQLManager().getWorldId(worldName);
		return plugin.getTempManager().cancelTemporaryPlayerPermission(playerId, worldId, permission);
	}

	public boolean groupAddTemporary(String groupName, String permission, int timeInSeconds) {
		int groupId = plugin.getGroupManager().getGroupId(groupName);
		if (groupId < 0) {
			return false;
		}
		return plugin.getTempManager().registerTemporaryGroupPermission(groupId, timeInSeconds, permission);
	}

	public boolean groupRemoveTemporary(String groupName, String permission) {
		int groupId = plugin.getGroupManager().getGroupId(groupName);
		if (groupId < 0) {
			return false;
		}
		return plugin.getTempManager().cancelTemporaryGroupPermission(groupId, permission);
	}

	public String getPlayerMeta(String worldName, String playerName, String node, String defaultValue)
	{
		Player p = Bukkit.getPlayerExact(playerName);
		String ret = null;
		if (p != null) {
			ret = plugin.getPlayerPermissions(p).getStringMeta(node, defaultValue);
		} else {
			int playerId = plugin.getSQLManager().getPlayerId(playerName);
			int worldId = plugin.getSQLManager().getWorldId(worldName);
			ret = plugin.getSQLManager().getPlayerMetaValue(playerId, worldId, node);
		}
		return ret;
	}

	public boolean setPlayerMeta(String worldName, String playerName, String node, String value)
	{
		Player p = Bukkit.getPlayerExact(playerName);
		int playerId;
		//= plugin.getSQLManager().getPlayerId(playerName, true);
		if(p == null) {
             playerId = plugin.getUuidManager().getOrCreateSqlUser(playerName);
            if(playerId < 0) {
                return false;
            }
		} else {
		    playerId = plugin.getPlayerPermissions(p).getId();
		}
		int worldId = plugin.getSQLManager().getWorldId(playerName, false);
		if (worldId < 0) {
			plugin.getSQLManager().setGlobalPlayerMeta(playerId, node, value);
		} else {
			plugin.getSQLManager().setPlayerMeta(playerId, worldId, node, value);
		}
		if (p != null) {
			plugin.getPlayerPermissions(p).recalculateMeta();
		}
		return true;
	}

	public String getGroupMeta(String world, String groupName, String node, String defaultValue)
	{
		Group group = plugin.getGroupManager().getGroup(groupName);
		if (group == null) {
			return defaultValue;
		}
		String value = group.getMeta(node);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}

	public void setGroupMeta(String world, String groupName, String node, String value)
	{
		Group group = plugin.getGroupManager().getGroup(groupName);
		if (group == null) {
			return;
		}
		group.setMeta(node, value);
		group.recalculatePermissions();
	}

	public boolean addGroupParent(String groupName, String parentName) {
		Group group = plugin.getGroupManager().getGroup(groupName);
		Group parent = plugin.getGroupManager().getGroup(parentName);
		if ((group == null) || (parent == null)) {
			return false;
		}
		for (Group g : parent.getAllParents()) {
			if (g.equals(group)) {
				return false; // no parent infinite loops.
			}
		}
		if (plugin.getSQLManager().addGroupParent(group.getId(), parent.getId())) {
			group.recalculateParents();
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param playerName The name of the player
	 * @param worldName The world to be checked, global if null.
	 * @param permission The permission to be checked.
	 * @return An iterable of messages that will best describe the origins of the particular node. Used in /playercheck.
	 */
	public Iterable<String> getPlayerPermissionInfo(String playerName, String worldName, String permission) {
		Player p = Bukkit.getPlayerExact(playerName);
		if (p == null) {
			int playerId = plugin.getSQLManager().getPlayerId(playerName);
			int worldId = plugin.getSQLManager().getWorldId(worldName);
			return plugin.getSQLManager().getPlayerNodeInfo(playerId, worldId, permission);
		} else {
			return plugin.getPlayerPermissions(p).getNodeInfo(permission);
		}
	}

	/**
	 * 
	 * @return An array of all groups defined across all worlds.
	 */
	public String[] getGroupsArray( ) {
		return Iterables.toArray(getGroups(), String.class);
	}

	/**
	 * 
	 * @return An iterable of all groups defined across all worlds.
	 */
	public Iterable<String> getGroups( ) {
		ArrayList<String> ret = new ArrayList<String>();
		for (Group g : plugin.getGroupManager().getGroups()) {
			ret.add(g.getName());
		}
		return ret;
	}
}