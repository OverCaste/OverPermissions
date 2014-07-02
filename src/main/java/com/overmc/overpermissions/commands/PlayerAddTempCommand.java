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

import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;
import com.overmc.overpermissions.TimeUtils;
import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.exceptions.TimeFormatException;

// ./playeraddtemp [player] [permission] [time] (world)
public class PlayerAddTempCommand implements TabExecutor {
    private final OverPermissions plugin;

    public PlayerAddTempCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerAddTempCommand register( ) {
        PluginCommand command = plugin.getCommand("playeraddtemp");
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
        if ((args.length < 3) || (args.length > 4)) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        final String playerName = args[0];
        final String permission = args[1];
        final String timeString = args[2];
        final String worldName = (args.length >= 4) ? args[3] : null;
        final boolean global = (worldName == null || "global".equals(worldName));
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                if (plugin.getUserManager().canUserExist(playerName)) {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_LOOKUP_FAILED, playerName));
                    return;
                }
                if (!global && Bukkit.getWorld(worldName) == null) { // Not global, and world doesn't exist.
                    sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, worldName));
                    return;
                }
                PermissionUser user = plugin.getUserManager().getPermissionUser(playerName);
                long time;
                try {
                    time = TimeUtils.parseMilliseconds(timeString);
                } catch (TimeFormatException ex) {
                    sender.sendMessage(Messages.format(ERROR_INVALID_TIME, timeString));
                    return;
                }
                if (global) {
                    if (user.addGlobalTempPermissionNode(permission, time, TimeUnit.MILLISECONDS)) {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADDTEMP_GLOBAL, permission, user, (time / 1000L)));
                    } else {
                        sender.sendMessage(Messages.format(ERROR_PLAYER_PERMISSION_ALREADY_SET_GLOBAL, permission));
                    }
                } else {
                    if (user.addTempPermissionNode(permission, worldName, time, TimeUnit.MILLISECONDS)) {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADDTEMP_WORLD, permission, user, CommandUtils.getWorldName(worldName), (time / 1000L)));
                    } else {
                        sender.sendMessage(Messages.format(ERROR_PLAYER_PERMISSION_ALREADY_SET_WORLD, permission, CommandUtils.getWorldName(worldName)));
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
            CommandUtils.loadPlayers(value, ret);
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
