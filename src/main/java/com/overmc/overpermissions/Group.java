package com.overmc.overpermissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;

public class Group implements Comparable<Group> {
	private final OverPermissions plugin;

	private final int id;
	private final String name;
	private int priority;
	private final ArrayList<Integer> parents = new ArrayList<Integer>(1);
	private final ArrayList<Integer> children = new ArrayList<Integer>(1);

	private final HashSet<PlayerPermissionData> playersInGroup = new HashSet<PlayerPermissionData>();
	private final HashMap<Integer, GroupWorldData> worldData = new HashMap<Integer, GroupWorldData>();
	private final HashSet<String> globalNodes = new HashSet<String>();
	private final HashMap<String, Boolean> globalPermissions = new HashMap<String, Boolean>();
	private final HashMap<String, String> globalMeta = new HashMap<String, String>();

	public Group(OverPermissions plugin, String name, int priority, int id) {
		this.id = id;
		this.name = name;
		this.plugin = plugin;
		this.priority = priority;
	}

	/**
	 * Checks for a permission in the global permission list.
	 * 
	 * @param permission The permission to check for
	 * @return true if it exists, false otherwise
	 */
	public boolean hasPermission(String permission) {
		return this.globalPermissions.containsKey(permission);
	}

	/**
	 * Checks for a permission in both the global permission list and the specified world's local permissions.
	 * 
	 * @param world The world to check permissions for
	 * @param permission The permission to check for
	 * @return true if it exists, false otherwise
	 */
	public boolean hasPermission(int world, String permission) {
		GroupWorldData d = this.worldData.get(world);
		if (d == null) {
			return hasPermission(permission);
		}
		return d.permissions.containsKey(permission) || hasPermission(permission);
	}

	/**
	 * Gets the permission value in the global permission list.
	 * 
	 * @param permission The permission to check the value of
	 * @return The value of the permission
	 */
	public boolean getPermission(String permission) {
		if (this.globalPermissions.containsKey(permission)) {
			return this.globalPermissions.get(permission);
		}
		return false;
	}

	/**
	 * Gets the permission value in both the global permission list and the specified world's local permissions.
	 * 
	 * @param world The world to check permissions for
	 * @param permission The permission node to check the value of
	 * @return The value of the permission
	 */
	public boolean getPermission(int world, String permission) {
		GroupWorldData d = this.worldData.get(world);
		if (d == null) {
			return getPermission(permission);
		}
		if (d.permissions.containsKey(permission)) {
			return d.permissions.get(permission);
		}
		return getPermission(permission);
	}

	/**
	 * Checks for a specified value in the global metadata list.
	 * 
	 * @param key The metadata to check for
	 * @return Whether or not the specified metadata value exists
	 */
	public boolean hasMeta(String key) {
		return this.globalMeta.containsKey(key);
	}

	/**
	 * Checks for a specified value in both the global metadata list and the specified world's metadata list.
	 * 
	 * @param world The world to check the metadata for
	 * @param key The metadata value to check for
	 * @return Whether or not the specifed metadata value exists
	 */
	public boolean hasMeta(int world, String key) {
		GroupWorldData d = this.worldData.get(world);
		if (d == null) {
			return hasMeta(key);
		}
		return d.meta.containsKey(key) || hasMeta(key);
	}

	public Collection<Map.Entry<String, String>> getAllMeta( ) {
		return this.globalMeta.entrySet();
	}

	public Collection<Map.Entry<String, String>> getAllMeta(int world) {
		GroupWorldData d = this.worldData.get(world);
		if (d == null) {
			return getAllMeta();
		}
		HashMap<String, String> ret = new HashMap<String, String>();
		ret.putAll(this.globalMeta);
		ret.putAll(d.meta);
		return ret.entrySet();
	}

	/**
	 * Retrieves the metadata value in the global list.
	 * 
	 * @param key The key of the metadata value to retrieve
	 * @return The value of the metadata in the global store at that location.
	 */
	public String getMeta(String key) {
		return this.globalMeta.get(key);
	}

	/**
	 * Retrieves the metadata value in either the local or the global list.
	 * 
	 * @param world The world to check for.
	 * @param key The key of the metadata value to retrieve.
	 * @return The value of the metadata in the specified world, or globally.
	 */
	public String getMeta(int world, String key) {
		GroupWorldData d = this.worldData.get(world);
		if ((d == null) || !d.meta.containsKey(key)) {
			return getMeta(key);
		}
		return d.meta.get(key);
	}

	public void recalculatePermissions( ) {
		this.priority = this.plugin.getSQLManager().getGroupPriority(this.id);
		for (GroupWorldData d : this.worldData.values()) { // Clear local permissions
			d.permissions.clear();
			d.nodes.clear();
		}
		this.globalPermissions.clear();
		this.globalNodes.clear(); // Clear global permissions
		for (int worldId : this.plugin.getSQLManager().getWorldIds()) { // Calculate the local nodes
			GroupWorldData d = this.worldData.get(worldId);
			List<String> groupNodes = this.plugin.getSQLManager().getGroupPermissions(this.id, worldId, false); // Don't retrieve the global nodes.
			groupNodes.addAll(this.plugin.getTempManager().getGroupNodes(this));
			if (!groupNodes.isEmpty()) {
				if (d == null) {
					d = new GroupWorldData();
					this.worldData.put(worldId, d);
				}
				d.nodes.addAll(groupNodes);
				d.permissions.putAll(PermissionUtils.getPermissionValues(groupNodes));
			}
		}
		this.globalNodes.addAll(this.plugin.getSQLManager().getGlobalGroupPermissions(this.id));
		for (PlayerPermissionData p : getAllPlayerChildren()) {
			p.recalculatePermissions();
		}
	}

	public void recalculateMeta( ) {
		for (GroupWorldData d : this.worldData.values()) { // Clear local meta
			d.meta.clear();
		}
		this.globalMeta.clear(); // Clear global meta
		for (int worldId : this.plugin.getSQLManager().getWorldIds()) {
			HashMap<String, String> meta = this.plugin.getSQLManager().getGroupMeta(this.id);
			if (!meta.isEmpty()) {
				GroupWorldData d = this.worldData.get(worldId);
				if (d == null) {
					d = new GroupWorldData();
					this.worldData.put(worldId, d);
				}
				d.meta.putAll(meta);
			}
		}
		for (PlayerPermissionData p : getAllPlayerChildren()) {
			p.recalculateMeta();
		}
	}

	public void recalculateParents( ) {
		this.parents.clear();
		this.parents.addAll(this.plugin.getSQLManager().getGroupParents(this.id));
		for (PlayerPermissionData p : this.playersInGroup) {
			p.recalculateGroups();
		}
		recalculateChildren();
		recalculatePermissions();
		recalculateMeta();
	}

	public void recalculateChildren( ) {
		this.children.clear();
		this.children.addAll(this.plugin.getSQLManager().getGroupChildren(this.id));
		for (Group group : getAllChildren()) {
			group.recalculateParents();
		}
		for (PlayerPermissionData p : getAllPlayerChildren()) {
			p.recalculateGroups();
		}
	}

	protected void addPlayerToGroup(PlayerPermissionData p) {
		this.playersInGroup.add(p);
	}

	protected void removePlayerFromGroup(PlayerPermissionData p) {
		this.playersInGroup.remove(p);
	}

	public Collection<Map.Entry<String, Boolean>> getPermissions(int worldId) {
		GroupWorldData d = this.worldData.get(worldId);
		if (d.permissions == null) {
			return new ArrayList<Map.Entry<String, Boolean>>(0);
		}
		return d.permissions.entrySet();
	}

	public Collection<Map.Entry<String, String>> getMeta(int worldId) {
		GroupWorldData d = this.worldData.get(worldId);
		if (d.permissions == null) {
			return new ArrayList<Map.Entry<String, String>>(0);
		}
		return d.meta.entrySet();
	}

	public Iterable<Group> getAllParents( ) {
		return getAllParents(new ArrayList<Group>());
	}

	protected Iterable<Group> getAllParents(ArrayList<Group> ret) {
		ret.add(this);
		for (Integer groupId : this.parents) {
			Group g = this.plugin.getGroupManager().getGroup(groupId);
			if ((g != null) && !ret.contains(g)) {
				g.getAllParents(ret);
			}
		}
		return ret;
	}

	public Collection<Group> getAllChildren( ) {
		return getAllChildren(new ArrayList<Group>());
	}

	protected Collection<Group> getAllChildren(ArrayList<Group> ret) {
		for (Integer groupId : this.children) {
			Group g = this.plugin.getGroupManager().getGroup(groupId);
			if ((g != null) && !ret.contains(g)) {
				ret.add(g);
				g.getAllChildren(ret);
			}
		}
		return ret;
	}

	public String getName( ) {
		return this.name;
	}

	public int getPriority( ) {
		return this.priority;
	}

	public int getId( ) {
		return this.id;
	}

	public Collection<String> getNodes(int worldId) {
		GroupWorldData d = this.worldData.get(worldId);
		if (d == null) {
			return new ArrayList<String>(0);
		}
		return d.nodes;
	}

	public boolean hasNode(int worldId, String node) {
		GroupWorldData d = this.worldData.get(worldId);
		if (d == null) {
			return false;
		}
		return d.nodes.contains(node);
	}

	public Collection<PlayerPermissionData> getPlayersInGroup( ) {
		return this.playersInGroup;
	}

	public Collection<PlayerPermissionData> getAllPlayerChildren( ) {
		HashSet<Group> allGroups = new HashSet<Group>();
		allGroups.add(this);
		allGroups.addAll(getAllChildren());
		HashSet<PlayerPermissionData> players = new HashSet<PlayerPermissionData>();
		for (Group g : allGroups) {
			players.addAll(g.getPlayersInGroup());
		}
		return players;
	}

	public Collection<Group> getChildren( ) {
		ArrayList<Group> ret = new ArrayList<Group>(this.children.size());
		for (Integer groupId : this.children) {
			ret.add(this.plugin.getGroupManager().getGroup(groupId));
		}
		return ret;
	}

	public Collection<Group> getParents( ) {
		ArrayList<Group> ret = new ArrayList<Group>(this.children.size());
		for (Integer groupId : this.parents) {
			ret.add(this.plugin.getGroupManager().getGroup(groupId));
		}
		return ret;
	}

	public String[] getDebugInfo( ) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<String> tempList = new ArrayList<String>();
		for (PlayerPermissionData d : this.playersInGroup) {
			tempList.add(d.getPlayerName());
		}
		if (tempList.size() > 0) {
			ret.add("Players in group: [" + Joiner.on(',').join(tempList) + "]");
		}
		tempList.clear();
		for (PlayerPermissionData d : getAllPlayerChildren()) {
			tempList.add(d.getPlayerName());
		}
		if (tempList.size() > 0) {
			ret.add("All player children in group: [" + Joiner.on(',').join(tempList) + "]");
		}
		tempList.clear();
		for (Integer i : this.parents) {
			Group other = this.plugin.getGroupManager().getGroup(i);
			if (other != null) {
				tempList.add(other.getName());
			}
		}
		if (tempList.size() > 0) {
			ret.add("Group parents: [" + Joiner.on(',').join(tempList) + "]");
		}
		tempList.clear();
		for (Integer i : this.children) {
			Group other = this.plugin.getGroupManager().getGroup(i);
			if (other != null) {
				tempList.add(other.getName());
			}
		}
		if (tempList.size() > 0) {
			ret.add("Group children: [" + Joiner.on(',').join(tempList) + "]");
		}
		tempList.clear();
		for (Group other : getAllChildren()) {
			if (other != null) {
				tempList.add(other.getName());
			}
		}
		if (tempList.size() > 0) {
			ret.add("All children: [" + Joiner.on(',').join(tempList) + "]");
		}
		tempList.clear();
		ret.add("Group priority: " + this.priority);
		return ret.toArray(new String[ret.size()]);
	}

	@Override
	public int compareTo(Group other) {
		return other.getPriority() - this.priority;
	}

	@Override
	public int hashCode( ) {
		return this.id;
	}

	@Override
	public boolean equals(Object other) {
		return ((other instanceof Group) && (((Group) other).id == this.id));
	}

	@Override
	public String toString( ) {
		return "Group [" + this.name + ", priority: " + this.priority + "]";
	}

	private class GroupWorldData {
		final HashSet<String> nodes = new HashSet<String>();
		final HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();
		final HashMap<String, String> meta = new HashMap<String, String>();
	}
}
