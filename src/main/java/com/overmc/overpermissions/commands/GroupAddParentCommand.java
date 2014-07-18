package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.events.GroupParentAddByPlayerEvent;
import com.overmc.overpermissions.api.events.GroupParentAddEvent;

// ./groupaddparent [group] [parent]
public class GroupAddParentCommand implements TabExecutor {
    private final OverPermissions plugin;

    public GroupAddParentCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public GroupAddParentCommand register( ) {
        PluginCommand command = plugin.getCommand("groupaddparent");
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
        if (args.length != 2) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        String groupName = args[0];
        String parentName = args[1];

        final PermissionGroup group = plugin.getGroupManager().getGroup(groupName);
        if (group == null) {
            sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, groupName));
            return true;
        }
        final PermissionGroup parent = plugin.getGroupManager().getGroup(parentName);
        if (parent == null) {
            sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, parentName));
            return true;
        }
        GroupParentAddEvent event;
        if (sender instanceof Player) {
            event = new GroupParentAddByPlayerEvent(group.getName(), parent.getName(), (Player) sender);
        } else {
            event = new GroupParentAddEvent(group.getName(), parent.getName());
        }
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                for (PermissionGroup g : parent.getAllParents()) {
                    if (g.equals(group)) {
                        sender.sendMessage(Messages.format(ERROR_PARENT_RECURSION, parent.getName(), group.getName()));
                        return;
                    }
                }
                if (group.addParent(parent)) {
                    sender.sendMessage(Messages.format(SUCCESS_GROUP_ADD_PARENT, parent.getName(), group.getName()));
                } else {
                    sender.sendMessage(Messages.format(ERROR_PARENT_ALREADY_SET, parent.getName()));
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
            CommandUtils.loadGroups(plugin.getGroupManager(), value, ret);
        } else if (index == 1) {
            CommandUtils.loadExclusiveGroup(plugin.getGroupManager(), value, args[0], ret);
        }
        return ret;
    }
}
