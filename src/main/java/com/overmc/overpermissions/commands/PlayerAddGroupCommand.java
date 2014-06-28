package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.Group;
import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;

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
    public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
        if (!sender.hasPermission(command.getPermission())) {
            sender.sendMessage(ERROR_NO_PERMISSION);
            return true;
        }
        if ((args.length < 2) || (args.length > 2)) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                String victim = args[0];
                int groupId = plugin.getSQLManager().getGroupId(args[1]);
                if (groupId < 0) {
                    sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, args[1]));
                    return;
                }
                int victimId = plugin.getUuidManager().getOrCreateSqlUser(victim);
                if (victimId < 0) {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_LOOKUP_FAILED, victim));
                    return;
                }
                if (plugin.getSQLManager().addPlayerGroup(victimId, groupId)) {
                    sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADD_GROUP, victim, args[1]));
                    @SuppressWarnings("deprecation")
                    Player p = Bukkit.getPlayerExact(victim);
                    if (p != null) {
                        plugin.getPlayerPermissions(p).recalculateGroups();
                    }
                } else {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_ALREADY_IN_GROUP, victim, args[1]));
                }
            }
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> ret = new ArrayList<String>();
        if (!sender.hasPermission(command.getPermission())) {
            sender.sendMessage(ERROR_NO_PERMISSION);
            return ret;
        }
        int index = args.length - 1;
        String value = args[index].toLowerCase();
        if (index == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(value)) {
                    ret.add(player.getName());
                }
            }
        } else if (index == 1) {
            for (Group g : plugin.getGroupManager().getGroups()) {
                ret.add(g.getName());
            }
        }
        return ret;
    }
}
