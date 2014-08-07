package com.overmc.overpermissions.internal.commands;

import static com.overmc.overpermissions.internal.Messages.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.events.GroupCreationByPlayerEvent;
import com.overmc.overpermissions.events.GroupCreationEvent;
import com.overmc.overpermissions.internal.Messages;
import com.overmc.overpermissions.internal.OverPermissions;

// ./groupcreate [group] [priority]
public class GroupCreateCommand implements TabExecutor {
    private final OverPermissions plugin;

    public GroupCreateCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public GroupCreateCommand register( ) {
        PluginCommand command = plugin.getCommand("groupcreate");
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
        final String groupName = args[0];
        final String priorityString = args[1];
        if (plugin.getGroupManager().doesGroupExist(groupName)) {
            sender.sendMessage(Messages.format(ERROR_GROUP_ALREADY_EXISTS, plugin.getGroupManager().getGroup(groupName).getName()));
            return true;
        }
        GroupCreationEvent event;
        if (sender instanceof Player) {
            event = new GroupCreationByPlayerEvent(groupName, (Player) sender);
        } else {
            event = new GroupCreationEvent(groupName);
        }
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                int priority;
                try {
                    priority = Integer.parseInt(priorityString);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Messages.format(ERROR_INVALID_INTEGER, priorityString));
                    return;
                }
                plugin.getGroupManager().createGroup(groupName, priority);
                sender.sendMessage(Messages.format(SUCCESS_GROUP_CREATE, groupName, priority));
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
        return ret;
    }
}
