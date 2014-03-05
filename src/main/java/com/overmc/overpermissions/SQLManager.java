package com.overmc.overpermissions;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public interface SQLManager {
	public static final int GLOBAL_WORLD_ID = 1;

	// @formatter:off
	// Global methods
	//  - World stuff
	/**
	 * Gets a list of all worlds loaded across all servers.
	 * @return
	 */
	public String[] getWorlds( );
	public String getWorldName(int id, int serverId);
	public int getWorldId(String worldname, int serverId, boolean makeNew);
	public int getWorldId(String worldname, int serverId);
	
	//  - Player stuff
	public String getPlayerName(int id);
	public int getPlayerId(String username, boolean makeNew);
	public int getPlayerId(String username);
	
	//  - Server stuff
	public int getNextServerId();
	
	// Player permission methods
	public List<String> getPlayerPermissions(int playerId, int worldId, boolean includeGlobals);
	public boolean checkPlayerPermission(int playerId, int worldIdId, int serverId, String permission);
	public Collection<String> getTotalPlayerNodes(int playerId, int worldId);
	public boolean checkPlayerPermissionExists(int playerId, int worldId, String permission);
	public boolean addPlayerPermission(int playerId, int worldId, String permission);
	public boolean addPlayerPermissionTimeout(int playerId, int worldId, String permission, long timeInSeconds);
	public boolean removePlayerPermission(int playerId, int worldId, String permission);
	public boolean removePlayerPermissionTimeout(int playerId, int worldId, String permission);
	public List<TimedPlayerPermission> getPlayerPermissionTimeouts(int playerId);
	
	// Player metadata methods
	public HashMap<String, String> getPlayerMeta(int playerId, int worldId);
	public String getPlayerMetaValue(int playerId, int worldId, String key);
	public void setPlayerMeta(int playerId, int world, String key, String value);
	public boolean delPlayerMeta(int playerId, int world, String key);
	public List<Integer> getPlayerGroups(int playerId);
	
	// Player group methods
	public List<Group> getPlayerEffectiveGroups(int playerId);
	public List<String> getPlayerNodeInfo(int playerId, int worldId, String node);
	public void setPlayerGroup(int playerId, int group);
	public boolean addPlayerGroup(int playerId, int groupId);
	public boolean isPlayerInGroup(int playerId, int groupId);
	public boolean removePlayerGroup(int playerId, int groupId);

	// Group general methods
	public boolean createGroup(String groupName, int priority);
	public void setGroupPriority(int groupId, int priority);
	public boolean deleteGroup(int groupId);
	public List<Group> getGroups( );
	public List<Integer> getGroupParents(int group);
	public List<Integer> getGroupChildren(int group);
	public boolean addGroupParent(int group, int parent);
	public boolean removeGroupParent(int group, int parent);
	public int getGroupId(String group);
	public int getGroupPriority(int groupId);
	public String getGroup(int groupId);

	// Group permission methods
	public boolean removeGroupPermission(int groupId, int worldId, String permission);
	public boolean removeGroupPermissionTimeout(int groupId, int worldId, String permission);
	public boolean addGroupPermission(int groupId, String permission, int worldId);
	public boolean addGroupPermissionTimeout(int groupId, String permission, int worldId, long timeout);
	public List<String> getGroupPermissions(int groupId, int worldId);
	public List<TimedGroupPermission> getGroupPermissionTimeouts(int groupId, int worldId);

	// Group meta methods
	public void setGroupMeta(int groupId, String key, String value);
	public boolean removeGroupMeta(int groupId, String key);
	public HashMap<String, String> getGroupMeta(int groupId);
	//@formatter:on
}
