package com.overmc.overpermissions.internal.commands;

import static com.overmc.overpermissions.internal.Messages.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.events.GroupPermissionAddByPlayerEvent;
import com.overmc.overpermissions.events.GroupPermissionAddEvent;
import com.overmc.overpermissions.internal.Messages;
import com.overmc.overpermissions.internal.OverPermissions;

// ./groupadd [group] [permission] (worldName)
public final class GroupAddCommand implements TabExecutor {
    private final OverPermissions plugin;

    public GroupAddCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public GroupAddCommand register( ) {
        PluginCommand command = plugin.getCommand("groupadd");
        command.setExecutor(this);
        command.setTabCompleter(this);
        return this;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(command.getPermission())) {
            sender.sendMessage(ERROR_NO_PERMISSION);
            return true;
        }
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        final String groupName = args[0];
        final String permissionNode = args[1];
        final String worldName = (args.length >= 3 ? args[2] : null);
        final boolean global = worldName == null || "global".equalsIgnoreCase(worldName);
        if (!plugin.getGroupManager().doesGroupExist(groupName)) {
            sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, groupName));
            return true;
        }
        final PermissionGroup group = plugin.getGroupManager().getGroup(groupName);
        if (!global && Bukkit.getWorld(worldName) == null) { // Not global, and world doesn't exist.
            sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, worldName));
            return true;
        }
        GroupPermissionAddEvent event;
        if (sender instanceof Player) {
            event = new GroupPermissionAddByPlayerEvent(group.getName(), worldName, permissionNode, (Player) sender);
        } else {
            event = new GroupPermissionAddEvent(group.getName(), worldName, permissionNode);
        }
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                if (global ? group.addGlobalPermissionNode(permissionNode) : group.addPermissionNode(permissionNode, CommandUtils.getWorldName(worldName))) {
                    if (global) {
                        sender.sendMessage(Messages.format(SUCCESS_GROUP_ADD_GLOBAL, permissionNode, group.getName()));
                    } else {
                        sender.sendMessage(Messages.format(SUCCESS_GROUP_ADD_WORLD, permissionNode, group.getName(), CommandUtils.getWorldName(worldName)));
                    }
                } else {
                    sender.sendMessage(Messages.format(ERROR_GROUP_PERMISSION_ALREADY_SET_GLOBAL, permissionNode));
                }
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> ret = new ArrayList<>();
        if (!sender.hasPermission(command.getPermission())) {
            return ret;
        }
        int index = args.length - 1;
        String value = args[index].toLowerCase();
        if (index == 0) {
            CommandUtils.loadGroups(plugin.getGroupManager(), value, ret);
        } else if (index == 1) {
            CommandUtils.loadPermissionNodes(value, ret);
        } else if (index == 2) {
            CommandUtils.loadWorlds(value, ret);
            CommandUtils.loadGlobalWorldConstant(value, ret);
        }
        return ret;
    }
}
