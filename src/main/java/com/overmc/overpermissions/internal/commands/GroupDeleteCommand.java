package com.overmc.overpermissions.internal.commands;

import static com.overmc.overpermissions.internal.Messages.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.events.GroupDeletionByPlayerEvent;
import com.overmc.overpermissions.events.GroupDeletionEvent;
import com.overmc.overpermissions.internal.Messages;
import com.overmc.overpermissions.internal.OverPermissions;

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
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(command.getPermission())) {
            sender.sendMessage(ERROR_NO_PERMISSION);
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        final String groupName = args[0];
        if (!plugin.getGroupManager().doesGroupExist(groupName)) {
            sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, groupName));
            return true;
        }
        final PermissionGroup group = plugin.getGroupManager().getGroup(groupName);
        if (plugin.getDefaultGroupName().equalsIgnoreCase(groupName)) {
            sender.sendMessage(Messages.format(ERROR_DELETE_DEFAULT_GROUP, group.getName()));
            return true;
        }
        if(!group.getParents().isEmpty()) {
            sender.sendMessage(Messages.format(ERROR_DELETE_GROUP_HAS_PARENTS, group.getName(), Joiner.on(", ").join(Iterables.transform(group.getParents(), new Function<PermissionGroup, String>() {
                @Override
                public String apply(PermissionGroup input) {
                    return input.getName();
                }
            }))));
            return true;
        }
        if(!group.getChildren().isEmpty()) {
            sender.sendMessage(Messages.format(ERROR_DELETE_GROUP_HAS_CHILDREN, group.getName(), Joiner.on(", ").join(Iterables.transform(group.getChildren(), new Function<PermissionGroup, String>() {
                @Override
                public String apply(PermissionGroup input) {
                    return input.getName();
                }
            }))));
            return true;
        }
        GroupDeletionEvent event;
        if (sender instanceof Player) {
            event = new GroupDeletionByPlayerEvent(group.getName(), (Player) sender);
        } else {
            event = new GroupDeletionEvent(group.getName());
        }
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                if (plugin.getGroupManager().deleteGroup(groupName)) {
                    sender.sendMessage(Messages.format(SUCCESS_GROUP_DELETE, group.getName()));
                } else {
                    sender.sendMessage(Messages.format(ERROR_UNKNOWN));
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
        }
        return ret;
    }
}
