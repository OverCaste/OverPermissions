package com.overmc.overpermissions.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;

import com.overmc.overpermissions.TimeUtils;
import com.overmc.overpermissions.api.GroupManager;
import com.overmc.overpermissions.api.MetadataBatch;
import com.overmc.overpermissions.api.NodeBatch;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.api.UserManager;

final class CommandUtils {
    private CommandUtils() {
        //Don't instantiate.
    }
    
    static void loadPermissionNodes(String currentValue, List<String> list) { //TODO properly cache instead of iterating through plugins every time.
        if (currentValue.startsWith("/")) {
            PluginCommand command = Bukkit.getPluginCommand(currentValue.substring(1)); //Remove trailing /
            if(command != null) {
                if(command.getPermission() == null) {
                    list.add("unknown");
                } else {
                    list.add(command.getPermission());
                }
            }
        } else {
            Set<String> permissions = new HashSet<String>(32);
            for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
                for (Permission perm : p.getDescription().getPermissions()) { //If the plugin is playing nicely, their permissions will be here.
                    String name = perm.getName().toLowerCase();
                    if (name.startsWith(currentValue)) {
                        permissions.add(name);
                    }
                }
                for(Map<String, Object> dataMap : p.getDescription().getCommands().values()) { //Otherwise, their permissions may be in the commands.
                    if(dataMap.containsKey("permission")) {
                        Object permission = dataMap.get("permission");
                        if(permission instanceof String) {
                            permissions.add((String)dataMap.get("permission"));
                        }
                    }
                }
            }
            list.addAll(permissions);
        }
    }
    
    static void loadGroupPermissionNodes(GroupManager groupManager, String currentValue, String groupName, List<String> list) {
        HashSet<String> uniqueNodes = new HashSet<String>();
        if (groupManager.doesGroupExist(groupName)) {
            PermissionGroup g = groupManager.getGroup(groupName);
            for (NodeBatch.Entry node : g.getAllPermissionNodes()) {
                if (node.getNode().toLowerCase().startsWith(currentValue)) {
                    uniqueNodes.add(node.getNode());
                }
            }
        }
        list.addAll(uniqueNodes);
    }
    
    static void loadPlayerPermissionNodes(UserManager userManager, String currentValue, String playerName, List<String> list) {
        HashSet<String> uniqueNodes = new HashSet<String>();
        if (userManager.doesUserExist(playerName)) {
            PermissionUser u = userManager.getPermissionUser(playerName);
            for (NodeBatch.Entry node : u.getAllPermissionNodes()) {
                if (node.getNode().toLowerCase().startsWith(currentValue)) {
                    uniqueNodes.add(node.getNode());
                }
            }
        }
        list.addAll(uniqueNodes);
    }
    
    static void loadGroupMetadata(GroupManager groupManager, String currentValue, String groupName, List<String> list) {
        HashSet<String> uniqueNodes = new HashSet<String>();
        if (groupManager.doesGroupExist(groupName)) {
            PermissionGroup g = groupManager.getGroup(groupName);
            for (MetadataBatch.Entry meta : g.getAllMetadata()) {
                uniqueNodes.add(meta.getKey());
            }
        }
        list.addAll(uniqueNodes);
    }
    
    static void loadPlayerMetdata(UserManager userManager, String currentValue, String playerName, List<String> list) {
        HashSet<String> uniqueNodes = new HashSet<String>();
        if (userManager.doesUserExist(playerName)) {
            PermissionUser u = userManager.getPermissionUser(playerName);
            for (MetadataBatch.Entry meta : u.getAllMetadata()) {
                uniqueNodes.add(meta.getKey());
            }
        }
        list.addAll(uniqueNodes);
    }
    
    static void loadPrefixMetadataConstant(String currentValue, List<String> list) {
        if("prefix".startsWith(currentValue) && !list.contains("prefix")) {
            list.add("prefix");
        }
    }
    
    static void loadSuffixMetadataConstant(String currentValue, List<String> list) {
        if("suffix".startsWith(currentValue) && !list.contains("suffix")) {
            list.add("suffix");
        }
    }
    
    static void loadWorlds(String currentValue, List<String> list) {
        for(World w : Bukkit.getWorlds()) {
            if(w.getName().toLowerCase().startsWith(currentValue)) {
                list.add(w.getName());
            }
        }
    }
    
    static void loadGlobalWorldConstant(String currentValue, List<String> list) {
        if("global".startsWith(currentValue) && !list.contains("global")) {
            list.add("global");
        }
    }
    
    static void loadGroups(GroupManager groupManager, String currentValue, List<String> list) {
        for (PermissionGroup g : groupManager.getGroups()) {
            if (g.getName().toLowerCase().startsWith(currentValue)) {
                list.add(g.getName());
            }
        }
    }
    
    static void loadExclusiveGroup(GroupManager groupManager, String currentValue, String exludedGroupName, List<String> list) {
        for (PermissionGroup g : groupManager.getGroups()) {
            String groupName = g.getName().toLowerCase();
            if (groupName.startsWith(currentValue) && !groupName.equalsIgnoreCase(exludedGroupName)) { // Can't match the first group, obviously.
                list.add(g.getName());
            }
        }
    }
    
    static void loadGroupParents(GroupManager groupManager, String currentValue, String groupName, List<String> list) {
        if(groupManager.doesGroupExist(groupName)) {
            HashSet<String> parents = new HashSet<>();
            PermissionGroup group = groupManager.getGroup(groupName);
            for(PermissionGroup parent : group.getParents()) {
                if(parent.getName().toLowerCase().startsWith(currentValue)) {
                   parents.add(parent.getName());
                }
            }
            list.addAll(parents);
        }
    }
    
    static void loadPlayerGroups(UserManager userManager, String currentValue, String userName, List<String> list) {
        if(userManager.doesUserExist(userName)) {
            HashSet<String> parents = new HashSet<>();
            PermissionUser user = userManager.getPermissionUser(userName);
            for(PermissionGroup parent : user.getParents()) {
                if(parent.getName().toLowerCase().startsWith(currentValue)) {
                    list.add(parent.getName());
                }
            }
            list.addAll(parents);
        }
    }
    
    static void loadPlayers(String currentValue, List<String> list) {
        for(Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(currentValue)) {
                list.add(p.getName());
            }
        }
    }
    
    static void loadTimeUnits(String currentValue, List<String> list) {
        char lastChar = currentValue.charAt(currentValue.length()-1);
        if(Character.isDigit(lastChar)) {
            list.addAll(TimeUtils.getTimeUnits());
        }
    }
    
    static void loadClearValueConstant(String currentValue, List<String> list) {
        if("clear".startsWith(currentValue) && !list.contains("clear")) {
            list.add("clear");
        }
    }
    
    static String getWorldName(String worldName) {
        return Bukkit.getWorld(worldName).getName();
    }
}
