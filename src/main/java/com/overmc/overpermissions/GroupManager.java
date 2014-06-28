package com.overmc.overpermissions;

import java.util.*;

public class GroupManager {
    private final OverPermissions plugin;
    private final HashMap<String, Group> groupNameMap = new HashMap<String, Group>();
    private final HashMap<Integer, Group> groupIdMap = new HashMap<Integer, Group>();

    public GroupManager(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public Group getGroup(String name) {
        return groupNameMap.get(name.toLowerCase());
    }

    public Group getGroup(int id) {
        return groupIdMap.get(id);
    }

    /**
     * 
     * @param name The name of the group who's id is being checked.
     * @return The id of the group if it exists, otherwise -1.
     */
    public int getGroupId(String name) {
        Group group = getGroup(name);
        if (group == null) {
            return -1;
        }
        return group.getId();
    }

    public Iterable<Group> getGroups( ) {
        return groupNameMap.values();
    }

    public boolean removeGroup(Group group) {
        if (groupNameMap.remove(group.getName()) != null) {
            groupIdMap.remove(group.getId());
            for (PlayerPermissionData player : group.getAllPlayerChildren()) {
                player.recalculateGroups();
            }
            return true;
        }
        return false;
    }

    public void recalculateGroups( ) {
        for (Group g : groupNameMap.values()) {
            plugin.getTempManager().deinit(g);
        }
        groupNameMap.clear();
        groupIdMap.clear();
        for (Group g : plugin.getSQLManager().getGroups()) {
            groupNameMap.put(g.getName().toLowerCase(), g);
            groupIdMap.put(g.getId(), g);
            plugin.getTempManager().init(g);
        }
        ArrayList<Group> groups = new ArrayList<Group>();
        groups.addAll(groupNameMap.values());
        Collections.sort(groups);
        Collections.reverse(groups);
        for (Group g : groupNameMap.values()) {
            g.recalculateParents();
            g.recalculateChildren();
        }
    }
}
