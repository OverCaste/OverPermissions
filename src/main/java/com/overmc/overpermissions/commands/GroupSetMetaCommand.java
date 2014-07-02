package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;

import com.google.common.base.Joiner;
import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;
import com.overmc.overpermissions.api.PermissionGroup;

// ./groupsetmeta [group] [key] (world) [value...]
public class GroupSetMetaCommand implements TabExecutor {
    private final OverPermissions plugin;

    public GroupSetMetaCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public GroupSetMetaCommand register( ) {
        PluginCommand command = plugin.getCommand("groupsetmeta");
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
        if (args.length < 3) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        final String worldName;
        final boolean global;
        boolean worldArgumentSet = false;
        if ("global".equals(args[2])) {
            worldName = "global";
            worldArgumentSet = true;
            global = true;
        } else if (Bukkit.getWorld(args[2]) != null) {
            worldName = Bukkit.getWorld(args[2]).getName();
            worldArgumentSet = true;
            global = false;
        } else {
            worldName = null;
            global = true;
        }
        if (worldArgumentSet == true && args.length < 4) { // Still no value, the world arg is optional
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        final String groupName = args[0];
        final String key = args[1];
        final String value = Joiner.on(' ').join(Arrays.copyOfRange(args, worldArgumentSet ? 3 : 2, args.length)); // If the world argument is set, skip it.
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                if (plugin.getGroupManager().doesGroupExist(groupName)) {
                    sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, groupName));
                    return;
                }
                if (!global && Bukkit.getWorld(worldName) == null) { // Not global, and world doesn't exist.
                    sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, worldName));
                    return;
                }
                PermissionGroup group = plugin.getGroupManager().getGroup(groupName);
                if ("clear".equalsIgnoreCase(value)) {
                    if (global) {
                        if (group.removeGlobalMeta(key)) {
                            sender.sendMessage(Messages.format(SUCCESS_GROUP_META_CLEAR_GLOBAL, key, group.getName()));
                        } else {
                            sender.sendMessage(Messages.format(ERROR_GROUP_UNKNOWN_META_KEY_GLOBAL, key, group.getName()));
                        }
                    } else {
                        if (group.removeMeta(key, worldName)) {
                            sender.sendMessage(Messages.format(SUCCESS_GROUP_META_CLEAR_WORLD, key, group.getName(), CommandUtils.getWorldName(worldName)));
                        } else {
                            sender.sendMessage(Messages.format(ERROR_GROUP_UNKNOWN_META_KEY_WORLD, key, group.getName(), CommandUtils.getWorldName(worldName)));
                        }
                    }
                } else {
                    if (global) {
                        group.setGlobalMeta(key, value);
                        sender.sendMessage(Messages.format(SUCCESS_GROUP_META_SET_GLOBAL, key, value, group.getName()));
                    } else {
                        group.setMeta(key, value, worldName);
                        sender.sendMessage(Messages.format(SUCCESS_GROUP_META_SET_WORLD, key, value, group.getName(), CommandUtils.getWorldName(worldName)));
                    }
                }
            }
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> ret = new ArrayList<String>();
        if (!sender.hasPermission(command.getPermission())) {
            return ret;
        }
        int index = args.length - 1;
        String value = args[index].toLowerCase();
        if (index == 0) {
            CommandUtils.loadGroups(plugin.getGroupManager(), value, ret);
        } else if (index == 1) {
            CommandUtils.loadGroupMetadata(plugin.getGroupManager(), value, args[0], ret);
            CommandUtils.loadPrefixMetadataConstant(value, ret);
            CommandUtils.loadSuffixMetadataConstant(value, ret);
        } else if (index == 2) {
            CommandUtils.loadWorlds(value, ret);
            CommandUtils.loadGlobalWorldConstant(value, ret);
        }
        if (index == 3 || index == 2) {
            CommandUtils.loadClearValueConstant(value, ret);
        }
        return ret;
    }
}
