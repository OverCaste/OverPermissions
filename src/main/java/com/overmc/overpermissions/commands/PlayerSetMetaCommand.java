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
import org.bukkit.entity.Player;

import com.google.common.base.Joiner;
import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;
import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.api.events.PlayerMetaAddByPlayerEvent;
import com.overmc.overpermissions.api.events.PlayerMetaAddEvent;
import com.overmc.overpermissions.api.events.PlayerMetaRemoveByPlayerEvent;
import com.overmc.overpermissions.api.events.PlayerMetaRemoveEvent;

// ./playersetmeta [player] [key] (world) [value]
public class PlayerSetMetaCommand implements TabExecutor {
    private final OverPermissions plugin;

    public PlayerSetMetaCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerSetMetaCommand register( ) {
        PluginCommand command = plugin.getCommand("playersetmeta");
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
        final String playerName = args[0];
        final String key = args[1];
        final String worldName;
        final String value;
        if (Bukkit.getWorld(args[2]) != null) {
            value = Joiner.on(' ').join(Arrays.copyOfRange(args, 3, args.length - 1));
            worldName = args[2];
        } else if ("global".equals(args[2])) {
            worldName = null;
            value = Joiner.on(' ').join(Arrays.copyOfRange(args, 3, args.length - 1));
        } else {
            worldName = null;
            value = Joiner.on(' ').join(Arrays.copyOfRange(args, 2, args.length - 1));
        }
        final boolean global = (worldName == null || "global".equals(worldName));
        if ("clear".equalsIgnoreCase(value)) {
            PlayerMetaRemoveEvent e;
            if (sender instanceof Player) {
                e = new PlayerMetaRemoveByPlayerEvent(playerName, worldName, key, (Player) sender);
            } else {
                e = new PlayerMetaRemoveEvent(playerName, worldName, key);
            }
            plugin.getServer().getPluginManager().callEvent(e);
            if (e.isCancelled()) {
                return true;
            }
        } else {
            PlayerMetaAddEvent e;
            if (sender instanceof Player) {
                e = new PlayerMetaAddByPlayerEvent(playerName, worldName, key, value, (Player) sender);
            } else {
                e = new PlayerMetaAddEvent(playerName, worldName, key, value);
            }
            plugin.getServer().getPluginManager().callEvent(e);
            if (e.isCancelled()) {
                return true;
            }
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                if (!global && Bukkit.getWorld(worldName) == null) { // Not global, and world doesn't exist.
                    sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, worldName));
                    return;
                }
                if (!plugin.getUserManager().canUserExist(playerName)) {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_LOOKUP_FAILED, playerName));
                    return;
                }
                PermissionUser user = plugin.getUserManager().getPermissionUser(playerName);
                if ("clear".equalsIgnoreCase(value)) {
                    if (global) {
                        if (user.removeGlobalMeta(key)) {
                            sender.sendMessage(Messages.format(SUCCESS_PLAYER_META_CLEAR_GLOBAL, key, user.getName()));
                        } else {
                            sender.sendMessage(Messages.format(ERROR_PLAYER_UNKNOWN_META_KEY_GLOBAL, key, user.getName()));
                        }
                    } else {
                        if (user.removeMeta(key, worldName)) {
                            sender.sendMessage(Messages.format(SUCCESS_PLAYER_META_CLEAR_WORLD, key, user.getName(), CommandUtils.getWorldName(worldName)));
                        } else {
                            sender.sendMessage(Messages.format(ERROR_PLAYER_UNKNOWN_META_KEY_WORLD, key, user.getName(), CommandUtils.getWorldName(worldName)));
                        }
                    }
                } else {
                    if (global) {
                        user.setGlobalMeta(key, value);
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_META_SET_GLOBAL, key, value, user.getName()));
                    } else {
                        user.setMeta(key, value, worldName);
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_META_SET_WORLD, key, value, user.getName(), CommandUtils.getWorldName(worldName)));
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
            sender.sendMessage(ERROR_NO_PERMISSION);
            return ret;
        }
        int index = args.length - 1;
        String value = args[index].toLowerCase();
        if (index == 0) {
            CommandUtils.loadPlayers(value, ret);
        } else if (index == 1) {
            CommandUtils.loadPlayerMetadata(plugin.getUserManager(), value, args[0], ret);
            CommandUtils.loadPrefixMetadataConstant(value, ret);
            CommandUtils.loadSuffixMetadataConstant(value, ret);
        } else if (index == 2) {
            CommandUtils.loadWorlds(value, ret);
            CommandUtils.loadGlobalWorldConstant(value, ret);
        } else if (index == 3) {
            CommandUtils.loadClearValueConstant(value, ret);
        }
        return ret;
    }
}
