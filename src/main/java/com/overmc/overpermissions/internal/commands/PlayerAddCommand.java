package com.overmc.overpermissions.internal.commands;

import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.events.PlayerPermissionAddByPlayerEvent;
import com.overmc.overpermissions.events.PlayerPermissionAddEvent;
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

// ./playeradd [player] [permission] (world)
public final class PlayerAddCommand implements TabExecutor {
    private final OverPermissions plugin;

    public PlayerAddCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerAddCommand register( ) {
        PluginCommand command = plugin.getCommand("playeradd");
        command.setExecutor(this);
        command.setTabCompleter(this);
        return this;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
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
        PlayerPermissionAddEvent event;
        if (sender instanceof Player) {
            event = new PlayerPermissionAddByPlayerEvent(playerName, worldName, permission, (Player) sender);
        } else {
            event = new PlayerPermissionAddEvent(playerName, worldName, permission);
        }
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                if (!plugin.getUserManager().canUserExist(playerName)) {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_INVALID_NAME, playerName));
                    return;
                }
                if (!global && Bukkit.getWorld(worldName) == null) { // Not global, and world doesn't exist.
                    sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, worldName));
                    return;
                }
                PermissionUser user = plugin.getUserManager().getPermissionUser(playerName);
                if (global ? user.addGlobalPermissionNode(permission) : user.addPermissionNode(permission, worldName)) {
                    if (global) {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADD_GLOBAL, permission, playerName));
                    } else {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADD_WORLD, permission, playerName, CommandUtils.getWorldName(worldName)));
                    }
                } else {
                    if (global) {
                        sender.sendMessage(Messages.format(ERROR_PLAYER_PERMISSION_ALREADY_SET_GLOBAL, permission));
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
            CommandUtils.loadWorlds(value, ret);
            CommandUtils.loadGlobalWorldConstant(value, ret);
        }
        return ret;
    }
}
