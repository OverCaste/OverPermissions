package com.overmc.overpermissions.internal.commands;

import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.events.PlayerPermissionRemoveByPlayerEvent;
import com.overmc.overpermissions.events.PlayerPermissionRemoveEvent;
import com.overmc.overpermissions.internal.Messages;
import com.overmc.overpermissions.internal.OverPermissions;
import com.overmc.overpermissions.internal.util.CommandUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static com.overmc.overpermissions.internal.Messages.*;

// ./playerremove [player] [permission] (world)
public class PlayerRemoveCommand implements TabExecutor {
    private final OverPermissions plugin;

    public PlayerRemoveCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerRemoveCommand register( ) {
        PluginCommand command = plugin.getCommand("playerremove");
        command.setExecutor(this);
        command.setTabCompleter(this);
        return this;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("overpermissions.playerremove")) {
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
        PlayerPermissionRemoveEvent e;
        if (sender instanceof Player) {
            e = new PlayerPermissionRemoveByPlayerEvent(playerName, worldName, permission, (Player) sender);
        } else {
            e = new PlayerPermissionRemoveEvent(playerName, worldName, permission);
        }
        plugin.getServer().getPluginManager().callEvent(e);
        if (e.isCancelled()) {
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                if (!plugin.getUserManager().doesUserExist(playerName)) {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_LOOKUP_FAILED, playerName));
                    return;
                }
                if (!global && Bukkit.getWorld(worldName) == null) { // Not global, and world doesn't exist.
                    sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, worldName));
                    return;
                }
                PermissionUser user = plugin.getUserManager().getPermissionUser(playerName);
                if (global) {
                    if (user.removeGlobalPermissionNode(permission)) {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_REMOVE_GLOBAL, permission, CommandUtils.getPlayerName(playerName)));
                    } else {
                        sender.sendMessage(Messages.format(ERROR_PLAYER_PERMISSION_NOT_SET_GLOBAL, permission));
                    }
                } else {
                    if (user.removePermissionNode(permission, worldName)) {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_REMOVE_WORLD, permission, CommandUtils.getPlayerName(playerName), CommandUtils.getWorldName(worldName)));
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
        int index = args.length - 1;
        String value = args[index].toLowerCase();
        if (index == 0) {
            CommandUtils.loadPlayers(value, ret);
        } else if (index == 1) {
            CommandUtils.loadPlayerPermissionNodes(plugin.getUserManager(), value, args[0], ret);
        } else if (index == 2) {
            CommandUtils.loadWorlds(value, ret);
            CommandUtils.loadGlobalWorldConstant(value, ret);
        }
        return ret;
    }
}
