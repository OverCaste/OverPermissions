package com.overmc.overpermissions;

import java.util.*;

import org.bukkit.World;

import com.google.common.base.Joiner;

public class Group implements Comparable<Group> {
	private final OverPermissions plugin;

	public final World world;
	private final int worldId;
	private final int id;
	private final String name;
	private int priority;
	private final ArrayList<Integer> parents = new ArrayList<Integer>(1);
	private final ArrayList<Integer> children = new ArrayList<Integer>(1);

	private final HashSet<String> nodes = new HashSet<String>();
	private final HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();
	private final HashMap<String, String> meta = new HashMap<String, String>();
	private final HashSet<PlayerPermissionData> playersInGroup = new HashSet<PlayerPermissionData>();

	public Group(OverPermissions plugin, World world, String name, int priority, int id) {
		this.world = world;
		worldId = (world == null) ? -1 : plugin.getSQLManager().getWorldId(world);
		this.id = id;
		this.name = name;
		this.plugin = plugin;
		this.priority = priority;
	}

	public boolean hasPermission(String permission) {
		return permissions.containsKey(permission);
	}

	public boolean getPermission(String permission) {
		return permissions.get(permission);
	}

	public boolean hasMeta(String key) {
		return meta.containsKey(key);
	}

	public String getMeta(String key) {
		return meta.get(key);
	}

	public void recalculatePermissions( ) {
		priority = plugin.getSQLManager().getGroupPriority(id);
		permissions.clear();
		nodes.clear();
		nodes.addAll(plugin.getSQLManager().getGroupPermissions(id));
		nodes.addAll(plugin.getTempManager().getGroupNodes(this));
		for (String permission : nodes) {
			if (permission.startsWith("-")) {
				permissions.put(permission.substring(1), false);
			} else {
				if (permission.startsWith("+")) { // regardless, +perm will ALWAYS be true
					permissions.put(permission.substring(1), true);
				} else {
					if (!nodes.contains("-" + permission)) { // doesn't contain neg
						permissions.put(permission, true);
					}
				}
			}
		}

		for (PlayerPermissionData p : getAllPlayerChildren()) {
			p.recalculatePermissions();
		}
	}

	public void recalculateMeta( ) {
		meta.clear();
		meta.putAll(plugin.getSQLManager().getGroupMeta(id));
		for (PlayerPermissionData p : getAllPlayerChildren()) {
			p.recalculateMeta();
		}
	}

	public void recalculateParents( ) {
		parents.clear();
		parents.addAll(plugin.getSQLManager().getGroupParents(id));
		for (PlayerPermissionData p : playersInGroup) {
			p.recalculateGroups();
		}
		recalculateChildren();
		recalculatePermissions();
		recalculateMeta();
	}

	public void recalculateChildren( ) {
		children.clear();
		children.addAll(plugin.getSQLManager().getGroupChildren(id));
		for (Group group : getAllChildren()) {
			group.recalculateParents();
		}
		for (PlayerPermissionData p : getAllPlayerChildren()) {
			p.recalculateGroups();
		}
	}

	protected void addPlayerToGroup(PlayerPermissionData p) {
		playersInGroup.add(p);
	}

	protected void removePlayerFromGroup(PlayerPermissionData p) {
		playersInGroup.remove(p);
	}

	public Collection<Map.Entry<String, Boolean>> getPermissions( ) {
		return permissions.entrySet();
	}

	public Set<Map.Entry<String, String>> getMeta( ) {
		return meta.entrySet();
	}

	public void setMeta(String node, String value) {
		if (value != null) {
			meta.put(node, value);
		}
	}

	public Iterable<Group> getAllParents( ) {
		return getAllParents(new ArrayList<Group>());
	}

	protected Iterable<Group> getAllParents(ArrayList<Group> ret) {
		ret.add(this);
		for (Integer groupId : parents) {
			Group g = plugin.getGroupManager().getGroup(groupId);
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
		for (Integer groupId : children) {
			Group g = plugin.getGroupManager().getGroup(groupId);
			if ((g != null) && !ret.contains(g)) {
				ret.add(g);
				g.getAllChildren(ret);
			}
		}
		return ret;
	}

	public String getName( ) {
		return name;
	}

	public World getWorld( ) {
		return world;
	}

	public int getPriority( ) {
		return priority;
	}

	public int getId( ) {
		return id;
	}

	public int getWorldId( ) {
		return worldId;
	}

	public Collection<String> getNodes( ) {
		return nodes;
	}

	public boolean hasNode(String node) {
		return nodes.contains(node);
	}

	public Collection<PlayerPermissionData> getPlayersInGroup( ) {
		return playersInGroup;
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
		ArrayList<Group> ret = new ArrayList<Group>(children.size());
		for (Integer groupId : children) {
			ret.add(plugin.getGroupManager().getGroup(groupId));
		}
		return ret;
	}

	public Collection<Group> getParents( ) {
		ArrayList<Group> ret = new ArrayList<Group>(children.size());
		for (Integer groupId : parents) {
			ret.add(plugin.getGroupManager().getGroup(groupId));
		}
		return ret;
	}

	public String[] getDebugInfo( ) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<String> tempList = new ArrayList<String>();
		for (PlayerPermissionData d : playersInGroup) {
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
		for (Integer i : parents) {
			Group other = plugin.getGroupManager().getGroup(i);
			if (other != null) {
				tempList.add(other.getName());
			}
		}
		if (tempList.size() > 0) {
			ret.add("Group parents: [" + Joiner.on(',').join(tempList) + "]");
		}
		tempList.clear();
		for (Integer i : children) {
			Group other = plugin.getGroupManager().getGroup(i);
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
		ret.add("Group priority: " + priority);
		return ret.toArray(new String[ret.size()]);
	}

	@Override
	public int compareTo(Group other) {
		return other.getPriority() - priority;
	}

	@Override
	public int hashCode( ) {
		return id;
	}

	@Override
	public boolean equals(Object other) {
		return ((other instanceof Group) && (((Group) other).id == id));
	}

	@Override
	public String toString( ) {
		return "Group [" + name + ", priority: " + priority + "]";
	}
}
