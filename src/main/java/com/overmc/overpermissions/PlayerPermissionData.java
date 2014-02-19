package com.overmc.overpermissions;

import java.util.*;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

public class PlayerPermissionData {
	private final OverPermissions plugin;

	private final Player player;
	private final int id;
	private int world;
	private PermissionAttachment values;
	private final HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();
	private final HashMap<String, Boolean> transientPermissions = new HashMap<String, Boolean>();
	private final HashMap<String, String> meta = new HashMap<String, String>();
	private final List<Group> groups = new ArrayList<Group>();

	public PlayerPermissionData(OverPermissions plugin, int id, int world, Player player) {
		this.plugin = plugin;
		this.player = player;
		this.id = id;
		this.world = world;
		recalculateGroups();
		recalculatePermissions();
	}

	public void recalculatePermissions( ) {
		unset();
		values = player.addAttachment(plugin);

		// permissions part
		permissions.clear();
		ArrayList<String> activeNodes = plugin.getSQLManager().getGlobalPlayerPermissions(id);
		for (Group group : getEffectiveGroups()) {
			group.addPlayerToGroup(this);
			activeNodes.addAll(group.getNodes());
		}
		activeNodes.addAll(plugin.getSQLManager().getPlayerPermissions(id, world));
		activeNodes.addAll(plugin.getTempManager().getPlayerNodes(player));
		HashMap<String, Boolean> parsedPermissions = PermissionUtils.getPermissionValues(activeNodes);
		permissions.putAll(parsedPermissions);
		permissions.putAll(transientPermissions);
		for (Map.Entry<String, Boolean> permEntry : permissions.entrySet()) {
			values.setPermission(permEntry.getKey(), permEntry.getValue());
		}
	}

	public void recalculateMeta( ) {
		meta.clear();
		for (Group group : getEffectiveGroups()) {
			for (Map.Entry<String, String> e : group.getMeta()) {
				meta.put(e.getKey(), e.getValue());
			}
		}
		meta.putAll(plugin.getSQLManager().getGlobalPlayerMeta(id));
		meta.putAll(plugin.getSQLManager().getPlayerMeta(id, world));
	}

	public void recalculateGroups( ) {
		for (Group g : plugin.getGroupManager().getGroups()) {
			g.removePlayerFromGroup(this);
		}
		groups.clear();
		for (Integer groupId : plugin.getSQLManager().getPlayerGroups(id)) {
			Group group = plugin.getGroupManager().getGroup(groupId);
			group.addPlayerToGroup(this);
			groups.add(group);
		}
	}

	public HashSet<String> getNodes( ) {
		HashSet<String> ret = new HashSet<String>();
		for (Group group : getEffectiveGroups()) {
			ret.addAll(group.getNodes());
		}
		ret.addAll(plugin.getSQLManager().getGlobalPlayerPermissions(id));
		ret.addAll(plugin.getSQLManager().getPlayerPermissions(id, world));
		ret.addAll(plugin.getTempManager().getPlayerNodes(player));
		return ret;
	}

	public ArrayList<String> getNodeInfo(String node) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<String> tempArray = new ArrayList<String>();
		boolean negSet = false;
		boolean posSet = false;
		for (Group group : getEffectiveGroups()) {
			for (String nodei : group.getNodes()) {
				String basenode = ((nodei.startsWith("-") || nodei.startsWith("+")) ? nodei.substring(1) : nodei);
				if (basenode.equalsIgnoreCase(node)) {
					if (nodei.startsWith("+") && !posSet) {
						tempArray.add(ChatColor.GREEN + nodei);
						posSet = true;
					} else if (nodei.startsWith("-") && !negSet && !posSet) {
						tempArray.add(ChatColor.RED + nodei);
						negSet = true;
					} else if (!negSet && !posSet) {
						tempArray.add(ChatColor.GREEN + nodei);
					}
				}
			}
		}
		if (tempArray.size() != 0) {
			ret.add(Messages.format(Messages.GROUPS_WITH_NODE));
			for (String s : tempArray) {
				ret.add(Messages.format(Messages.GROUP_NODE_VALUE, s));
			}
		}
		tempArray.clear();
		ArrayList<String> effectiveNodes = plugin.getSQLManager().getGlobalPlayerPermissions(id);
		effectiveNodes.addAll(plugin.getSQLManager().getPlayerPermissions(id, world));
		for (String nodei : effectiveNodes) {
			String basenode = ((nodei.startsWith("-") || nodei.startsWith("+")) ? nodei.substring(1) : nodei);
			if (basenode.equalsIgnoreCase(node)) {
				if (nodei.startsWith("+") && !posSet) {
					tempArray.add(ChatColor.GREEN + nodei);
					posSet = true;
				} else if (nodei.startsWith("-") && !negSet && !posSet) {
					tempArray.add(ChatColor.RED + nodei);
					negSet = true;
				} else if (!negSet && !posSet) {
					tempArray.add(ChatColor.GREEN + nodei);
				}
			}
		}
		if (tempArray.size() != 0) {
			ret.add(Messages.format(Messages.PLAYER_NODES));
			for (String s : tempArray) {
				ret.add(Messages.format(Messages.PLAYER_NODE_VALUE, s));
			}
		}
		tempArray.clear();
		for (String nodei : plugin.getTempManager().getPlayerNodes(player)) {
			String nodeival = ((nodei.startsWith("-") || nodei.startsWith("+")) ? nodei.substring(1) : nodei);
			if (nodeival.equalsIgnoreCase(node)) {
				if (nodei.startsWith("+") && !posSet) {
					tempArray.add(ChatColor.GREEN + nodei);
					posSet = true;
				} else if (nodei.startsWith("-") && !negSet && !posSet) {
					tempArray.add(ChatColor.RED + nodei);
					negSet = true;
				} else if (!negSet && !posSet) {
					tempArray.add(ChatColor.GREEN + nodei);
				}
			}
		}
		if (tempArray.size() != 0) {
			ret.add(Messages.format(Messages.PLAYER_TEMP_NODES));
			for (String s : tempArray) {
				ret.add(Messages.format(Messages.PLAYER_TEMP_NODE_VALUE, s));
			}
		}
		if (transientPermissions.containsKey(node)) {
			ret.add(Messages.format(Messages.PLAYER_TRANSIENT_NODES));
			boolean value = transientPermissions.get(node);
			ret.add(Messages.format(Messages.PLAYER_TRANSIENT_NODE_VALUE, (value ? ChatColor.GREEN : ChatColor.RED) + ": " + Boolean.valueOf(value).toString()));
		}
		return ret;
	}

	public Map<String, String> getMeta( ) {
		return Collections.unmodifiableMap(meta);
	}

	public Boolean getGroupPermission(String key) {
		Boolean ret = null;
		for (Group group : getEffectiveGroups()) {
			if ((group.getWorld() == null) || group.getWorld().equals(player.getWorld())) { // groups have lower priority
				if (group.hasPermission(key)) {
					ret = group.getPermission(key);
				}
			}
		}
		return ret;
	}

	public ArrayList<Group> getEffectiveGroups( ) {
		ArrayList<Group> effectiveGroups = new ArrayList<Group>();
		for (Group group : groups) {
			for (Group otherGroup : group.getAllParents()) {
				if (!effectiveGroups.contains(otherGroup) && ((otherGroup.getWorld() == null) || otherGroup.getWorld().equals(world))) {
					effectiveGroups.add(otherGroup);
				}
			}
		}
		Collections.sort(effectiveGroups);
		return effectiveGroups;
	}

	public List<Group> getGroups( ) {
		return Collections.unmodifiableList(groups);
	}

	public void unset( ) {
		if (values != null) {
			player.removeAttachment(values);
			for (String perm : values.getPermissions().keySet()) {
				values.unsetPermission(perm);
			}
		}
		values = null;
	}

	public String getStringMeta(String key, String defaultValue) {
		try {
			String s = meta.get(key);
			if (s == null) {
				return defaultValue;
			}
			return s;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public boolean getPermission(String key) {
		return player.hasPermission(key);
	}

	public void setTransient(String key, boolean value) {
		transientPermissions.put(key, value);
		values.setPermission(key, value);
	}

	public boolean clearTransient(String key) {
		if (transientPermissions.containsKey(key)) {
			transientPermissions.remove(key);
			recalculatePermissions();
			return true;
		}
		return false;
	}

	public boolean isInGroup(Group g) {
		return groups.contains(g);
	}

	public int getId( ) {
		return id;
	}

	public void setWorld(int world) {
		this.world = world;
	}

	public int getWorld( ) {
		return world;
	}

	public String getPlayerName( ) {
		return player.getName();
	}

	public Player getPlayer( ) {
		return player;
	}

	@Override
	public int hashCode( ) {
		return id;
	}

	@Override
	public boolean equals(Object other) {
		return ((other instanceof PlayerPermissionData) && (((PlayerPermissionData) other).id == id));
	}
}
