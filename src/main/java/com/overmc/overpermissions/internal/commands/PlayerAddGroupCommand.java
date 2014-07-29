package com.overmc.overpermissions.internal.commands;

import static com.overmc.overpermissions.internal.Messages.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.events.PlayerGroupAddByPlayerEvent;
import com.overmc.overpermissions.events.PlayerGroupAddEvent;
import com.overmc.overpermissions.internal.Messages;
import com.overmc.overpermissions.internal.OverPermissions;

// ./playeraddgroup [player] [group]
public class PlayerAddGroupCommand implements TabExecutor {
    private final OverPermissions plugin;

    public PlayerAddGroupCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerAddGroupCommand register( ) {
        PluginCommand command = plugin.getCommand("playeraddgroup");
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
        if ((args.length < 2) || (args.length > 2)) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        final String playerName = args[0];
        final String groupName = args[1];
        if (!plugin.getGroupManager().doesGroupExist(groupName)) {
            sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, groupName));
            return true;
        }
        final PermissionGroup group = plugin.getGroupManager().getGroup(groupName);
        if (!plugin.getUserManager().canUserExist(playerName)) {
            sender.sendMessage(Messages.format(ERROR_PLAYER_INVALID_NAME, playerName));
            return true;
        }
        PlayerGroupAddEvent event;
        if (sender instanceof Player) {
            event = new PlayerGroupAddByPlayerEvent(playerName, group.getName(), (Player) sender);
        } else {
            event = new PlayerGroupAddEvent(playerName, group.getName());
        }
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                PermissionUser victim = plugin.getUserManager().getPermissionUser(playerName);
                if (victim.addParent(group)) {
                    sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADD_GROUP, group.getName(), playerName));
                } else {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_ALREADY_IN_GROUP, playerName, group.getName()));
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
            ;
        } else if (index == 1) {
            CommandUtils.loadGroups(plugin.getGroupManager(), value, ret);
        }
        return ret;
    }
}
