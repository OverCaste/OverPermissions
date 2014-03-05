package com.overmc.overpermissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Group implements Comparable<Group> {
	private final OverPermissions plugin;

	private final int id;
	private final String name;
	private int priority;
	private final ArrayList<Integer> parents = new ArrayList<Integer>(1);
	private final ArrayList<Integer> children = new ArrayList<Integer>(1);

	private final Multimap<Integer, String> nodes = HashMultimap.create();
	private final HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();
	private final HashMap<String, String> meta = new HashMap<String, String>();
	private final HashSet<PlayerPermissionData> playersInGroup = new HashSet<PlayerPermissionData>();

	public Group(OverPermissions plugin, String name, int priority, int id) {
		this.id = id;
		this.name = name;
		this.plugin = plugin;
		this.priority = priority;
	}

	public boolean hasPermission(String permission) {
		return this.permissions.containsKey(permission);
	}

	public boolean getPermission(String permission) {
		return this.permissions.get(permission);
	}

	public boolean hasMeta(String key) {
		return this.meta.containsKey(key);
	}

	public String getMeta(String key) {
		return this.meta.get(key);
	}

	public void recalculatePermissions( ) {
		this.priority = this.plugin.getSQLManager().getGroupPriority(this.id);
		this.permissions.clear();
		this.nodes.clear();
		this.nodes.addAll(this.plugin.getSQLManager().getGroupPermissions(this.id));
		this.nodes.addAll(this.plugin.getTempManager().getGroupNodes(this));
		for (String permission : this.nodes) {
			if (permission.startsWith("-")) {
				this.permissions.put(permission.substring(1), false);
			} else {
				if (permission.startsWith("+")) { // regardless, +perm will ALWAYS be true
					this.permissions.put(permission.substring(1), true);
				} else {
					if (!this.nodes.contains("-" + permission)) { // doesn't contain neg
						this.permissions.put(permission, true);
					}
				}
			}
		}

		for (PlayerPermissionData p : getAllPlayerChildren()) {
			p.recalculatePermissions();
		}
	}

	public void recalculateMeta( ) {
		this.meta.clear();
		this.meta.putAll(this.plugin.getSQLManager().getGroupMeta(this.id));
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

	public Collection<Map.Entry<String, Boolean>> getPermissions( ) {
		return this.permissions.entrySet();
	}

	public Set<Map.Entry<String, String>> getMeta( ) {
		return this.meta.entrySet();
	}

	public void setMeta(String node, String value) {
		if (value != null) {
			this.meta.put(node, value);
		}
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

	public Collection<String> getNodes( ) {
		return this.nodes;
	}

	public boolean hasNode(String node, int worldId) {
		return this.nodes.contains(node);
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
}
