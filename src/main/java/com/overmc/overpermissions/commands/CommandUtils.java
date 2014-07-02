package com.overmc.overpermissions.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;

import com.overmc.overpermissions.api.GroupManager;
import com.overmc.overpermissions.api.PermissionGroup;

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
            
        }
    }
    
    static void loadWorlds(String currentValue, List<String> list) {
        for(World w : Bukkit.getWorlds()) {
            if(w.getName().toLowerCase().startsWith(currentValue)) {
                list.add(w.getName());
            }
        }
    }
    
    static void loadGlobal(String currentValue, List<String> list) {
        if("global".startsWith(currentValue)) {
            list.add("global");
        }
    }
    
    static void loadGroup(GroupManager groupManager, String currentValue, List<String> list) {
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
}
