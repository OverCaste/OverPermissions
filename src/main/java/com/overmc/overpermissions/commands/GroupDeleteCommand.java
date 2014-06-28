package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.*;

import org.bukkit.command.*;

import com.overmc.overpermissions.*;

// ./groupdelete [group]
public class GroupDeleteCommand implements TabExecutor {
    private final OverPermissions plugin;

    public GroupDeleteCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public GroupDeleteCommand register( ) {
        PluginCommand command = plugin.getCommand("groupdelete");
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
        if (args.length != 1) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        Group group = plugin.getGroupManager().getGroup(args[0]);
        if (group == null) {
            sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, args[0]));
            return true;
        }
        if (plugin.getDefaultGroupId() == group.getId()) {
            sender.sendMessage(Messages.format(ERROR_DELETE_DEFAULT_GROUP, args[0]));
            return true;
        }
        if (plugin.getSQLManager().deleteGroup(group.getId())) {
            sender.sendMessage(Messages.format(SUCCESS_GROUP_DELETE, args[0]));
            plugin.getGroupManager().recalculateGroups();
        } else {
            sender.sendMessage(Messages.format(ERROR_UNKNOWN));
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
        }
        return ret;
    }
}
