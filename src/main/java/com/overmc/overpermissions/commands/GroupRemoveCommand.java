package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.*;

import org.bukkit.command.*;

import com.overmc.overpermissions.*;

// ./groupremove [group] [permission]
public class GroupRemoveCommand implements TabExecutor {
    private final OverPermissions plugin;

    public GroupRemoveCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public GroupRemoveCommand register( ) {
        PluginCommand command = plugin.getCommand("groupremove");
        command.setExecutor(this);
        command.setTabCompleter(this);
        return this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(command.getPermission())) {
            sender.sendMessage(ERROR_NO_PERMISSION);
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        Group group = plugin.getGroupManager().getGroup(args[0]);
        if (group == null) {
            sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, args[0]));
            return true;
        }
        if (plugin.getSQLManager().removeGroupPermission(group.getId(), args[1])) {
            sender.sendMessage(Messages.format(SUCCESS_GROUP_REMOVE, args[1], group.getName()));
            group.recalculatePermissions();
        } else {
            sender.sendMessage(Messages.format(ERROR_GROUP_PERMISSION_NOT_SET, args[1]));
        }
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
            for (Group g : plugin.getGroupManager().getGroups()) {
                if (g.getName().toLowerCase().startsWith(value)) {
                    ret.add(g.getName());
                }
            }
        } else if (index == 1) {
            Group g = plugin.getGroupManager().getGroup(args[0]);
            if (g != null) {
                for (String node : g.getNodes()) {
                    if (node.toLowerCase().startsWith(value)) {
                        ret.add(node);
                    }
                }
            }
        }
        return ret;
    }
}
