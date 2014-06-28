package com.overmc.overpermissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.World;

public interface SQLManager {
    public ArrayList<String> getPlayerPermissions(int playerId, int worldId);

    public ArrayList<String> getGlobalPlayerPermissions(int playerId);

    public List<Group> getPlayerEffectiveGroups(int playerId, int worldId);

    public boolean checkPlayerPermission(int playerId, int worldId, String permission);

    public List<String> getPlayerNodeInfo(int playerId, int worldId, String node);

    public Collection<String> getTotalPlayerNodes(int playerId, int worldId);

    public boolean checkPlayerPermissionExists(int playerId, int worldId, String permission);

    public HashMap<String, String> getPlayerMeta(int playerId, int worldId);

    public HashMap<String, String> getGlobalPlayerMeta(int playerId);

    public String getPlayerMetaValue(int playerId, int worldId, String key);

    public boolean addPlayerPermission(int playerId, int worldId, String permission);

    public boolean addGlobalPlayerPermission(int playerId, String permission);

    public boolean addPlayerPermissionTimeout(int playerId, int worldId, String permission, long timeInSeconds);

    public boolean addGlobalPlayerPermissionTimeout(int playerId, String permission, long timeInSeconds);

    public boolean removePlayerPermissionTimeout(int playerId, int worldId, String permission);

    public boolean removeGlobalPlayerPermissionTimeout(int playerId, String permission);

    public ArrayList<TimedPlayerPermission> getPlayerPermissionTimeouts(int playerId);

    public boolean removePlayerPermission(int playerId, int worldId, String permission);

    public boolean removeGlobalPlayerPermission(int playerId, String permission);

    public void setPlayerMeta(int playerId, int world, String key, String value);

    public void setGlobalPlayerMeta(int playerId, String key, String value);

    public boolean delPlayerMeta(int playerId, int world, String key);

    public boolean delGlobalPlayerMeta(int playerId, String key);

    public ArrayList<Integer> getPlayerGroups(int playerId);

    public void setPlayerGroup(int playerId, int group);

    public boolean addPlayerGroup(int playerId, int groupId);

    public boolean isPlayerInGroup(int playerId, int groupId);

    public boolean removePlayerGroup(int playerId, int groupId);

    public boolean setGroup(String groupName, int priority, int world);

    public boolean deleteGroup(int groupId);

    public ArrayList<Group> getGroups( );

    public ArrayList<Integer> getGroupParents(int group);

    public ArrayList<Integer> getGroupChildren(int group);

    public boolean addGroupParent(int group, int parent);

    public boolean removeGroupParent(int group, int parent);

    public boolean removeGroupPermission(int groupId, String permission);

    public boolean removeGroupPermissionTimeout(int groupId, String permission);

    public boolean addGroupPermission(int groupId, String permission);

    public boolean addGroupPermissionTimeout(int groupId, String permission, long timeout);

    public void setGroupMeta(int groupId, String key, String value);

    public boolean removeGroupMeta(int groupId, String key);

    public int getGroupId(String group);

    public int getGroupPriority(int groupId);

    public ArrayList<String> getGroupPermissions(int groupId);

    public ArrayList<TimedGroupPermission> getGroupPermissionTimeouts(int groupId);

    public HashMap<String, String> getGroupMeta(int groupId);

    public String getGroup(int groupId);

    public int getPermissionValue(String permission, boolean insertIfNotExists);

    public int getPermissionValue(String permission);

    public String getPermissionValue(int permissionId);

    public String[] getWorlds( );

    public org.bukkit.World getWorld(int id);

    public String getWorldName(int id);

    public int getWorldId(String worldname, boolean makeNew);

    public int getWorldId(String worldname);

    public int getWorldId(World world);

    public int getPlayerId(UUID uuid, boolean makeNew);

    public int getPlayerId(UUID uuid);

    public int getPlayerId(String username);

    public UUID getPlayerUuid(int id);

    public org.bukkit.entity.Player getPlayer(int id);

    public String getLastSeenPlayerName(int id);

    public void deleteDatabase( );
}
