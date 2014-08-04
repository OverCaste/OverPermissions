package com.overmc.overpermissions.internal.commands;

import static com.overmc.overpermissions.internal.Messages.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.events.PlayerPermissionRemoveByPlayerEvent;
import com.overmc.overpermissions.events.PlayerPermissionRemoveEvent;
import com.overmc.overpermissions.internal.Messages;
import com.overmc.overpermissions.internal.OverPermissions;

// ./playeraddtemp [player] [permission] (world)
public class PlayerRemoveTempCommand implements TabExecutor {
    private final OverPermissions plugin;

    public PlayerRemoveTempCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerRemoveTempCommand register( ) {
        PluginCommand command = plugin.getCommand("playerremovetemp");
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
        if ((args.length < 2) || (args.length > 3)) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        final String playerName = args[0];
        final String permission = args[1];
        final String worldName = (args.length >= 3) ? args[2] : null;
        final boolean global = (worldName == null || "global".equals(worldName));
        if (!plugin.getUserManager().canUserExist(playerName)) {
            sender.sendMessage(Messages.format(ERROR_PLAYER_INVALID_NAME, playerName));
            return true;
        }
        if (!global && Bukkit.getWorld(worldName) == null) { // Not global, and world doesn't exist.
            sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, worldName));
            return true;
        }
        final PermissionUser user = plugin.getUserManager().getPermissionUser(playerName);
        PlayerPermissionRemoveEvent event;
        if (sender instanceof Player) {
            event = new PlayerPermissionRemoveByPlayerEvent(playerName, worldName, permission, true, (Player) sender);
        } else {
            event = new PlayerPermissionRemoveEvent(playerName, worldName, permission, true);
        }
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                if (global) {
                    if (user.removeGlobalTempPermissionNode(permission)) {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADDTEMP_GLOBAL, permission, CommandUtils.getPlayerName(playerName)));
                    } else {
                        sender.sendMessage(Messages.format(ERROR_PLAYER_PERMISSION_NOT_SET_GLOBAL, permission));
                    }
                } else {
                    if (user.removeTempPermissionNode(permission, worldName)) {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADDTEMP_WORLD, permission, CommandUtils.getPlayerName(playerName), CommandUtils.getWorldName(worldName)));
                    } else {
                        sender.sendMessage(Messages.format(ERROR_PLAYER_PERMISSION_NOT_SET_WORLD, permission, CommandUtils.getWorldName(worldName)));
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
