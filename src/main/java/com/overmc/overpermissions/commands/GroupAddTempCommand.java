package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;
import com.overmc.overpermissions.TimeUtils;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.events.GroupPermissionAddByPlayerEvent;
import com.overmc.overpermissions.api.events.GroupPermissionAddEvent;
import com.overmc.overpermissions.exceptions.TimeFormatException;

// ./groupaddtemp [group] [permission] [time] (world)
public class GroupAddTempCommand implements TabExecutor {
    private final OverPermissions plugin;

    public GroupAddTempCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public GroupAddTempCommand register( ) {
        PluginCommand command = plugin.getCommand("groupaddtemp");
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
        if (args.length < 3 || args.length > 4) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        String groupName = args[0];
        final String permissionNode = args[1];
        final String worldName = (args.length >= 4 ? args[3] : null);
        final boolean global = worldName == null;
        final PermissionGroup group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) {
            sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, groupName));
            return true;
        }
        if (!global && Bukkit.getWorld(worldName) == null) { // Not global, and world doesn't exist.
            sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, worldName));
            return true;
        }
        final long timeInMillis;
        try {
            timeInMillis = TimeUtils.parseMilliseconds(args[2]) + System.currentTimeMillis();
        } catch (TimeFormatException e) {
            sender.sendMessage(Messages.format(ERROR_INVALID_TIME, args[2]));
            return true;
        }
        GroupPermissionAddEvent event;
        if (sender instanceof Player) {
            event = new GroupPermissionAddByPlayerEvent(groupName, worldName, permissionNode, true, (Player) sender);
        } else {
            event = new GroupPermissionAddEvent(groupName, worldName, permissionNode, true);
        }
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                if (global) {
                    if (group.addGlobalTempPermissionNode(permissionNode, timeInMillis, TimeUnit.MILLISECONDS)) {
                        sender.sendMessage(Messages.format(SUCCESS_GROUP_ADD_TEMP_GLOBAL, permissionNode, group.getName(), TimeUnit.MILLISECONDS.toSeconds(timeInMillis)));
                    } else {
                        sender.sendMessage(Messages.format(ERROR_GROUP_PERMISSION_ALREADY_SET_GLOBAL, permissionNode));
                    }
                } else {
                    if (group.addTempPermissionNode(permissionNode, worldName, timeInMillis, TimeUnit.MILLISECONDS)) {
                        sender.sendMessage(Messages.format(SUCCESS_GROUP_ADD_TEMP_WORLD, permissionNode, group.getName(), TimeUnit.MILLISECONDS.toSeconds(timeInMillis), CommandUtils
                                .getWorldName(worldName)));
                    } else {
                        sender.sendMessage(Messages.format(ERROR_GROUP_PERMISSION_ALREADY_SET_WORLD, permissionNode, CommandUtils.getWorldName(worldName)));
                    }
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
            CommandUtils.loadTimeUnits(value, ret);
        } else if (index == 3) {
            CommandUtils.loadWorlds(value, ret);
            CommandUtils.loadGlobalWorldConstant(value, ret);
        }
        return ret;
    }
}
