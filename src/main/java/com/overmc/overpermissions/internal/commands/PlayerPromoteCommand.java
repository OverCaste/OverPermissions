package com.overmc.overpermissions.internal.commands;

import static com.overmc.overpermissions.internal.Messages.*;

import java.util.*;

import org.bukkit.command.*;

import com.google.common.collect.Iterables;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.internal.Messages;
import com.overmc.overpermissions.internal.OverPermissions;

// ./playerpromote [player] (choice)
public class PlayerPromoteCommand implements TabExecutor {
    private final OverPermissions plugin;

    public PlayerPromoteCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerPromoteCommand register( ) {
        PluginCommand command = plugin.getCommand("playerpromote");
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
        if ((args.length < 1) || (args.length > 2)) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        final String playerName = args[0];
        final String choice = (args.length >= 2) ? args[1] : null;
        if (!plugin.getUserManager().canUserExist(playerName)) {
            sender.sendMessage(Messages.format(ERROR_PLAYER_INVALID_NAME, playerName));
            return true;
        }
        final PermissionUser user = plugin.getUserManager().getPermissionUser(playerName);
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                PermissionGroup playerOnlyGroup = null;
                for (PermissionGroup g : user.getParents()) {
                    if (playerOnlyGroup != null) {
                        sender.sendMessage(Messages.format(ERROR_PROMOTE_PLAYER_MULTIPLE_GROUPS, CommandUtils.getPlayerName(playerName)));
                        return;
                    } else {
                        playerOnlyGroup = g;
                    }
                }
                if (playerOnlyGroup == null) {
                    throw new AssertionError("A player wasn't in any groups! (" + CommandUtils.getPlayerName(playerName) + ")");
                }
                Collection<PermissionGroup> children = playerOnlyGroup.getChildren();
                if (children.size() == 0) {
                    sender.sendMessage(Messages.format(ERROR_GROUP_NO_CHILDREN, playerOnlyGroup.getName()));
                } else if (children.size() == 1) {
                    PermissionGroup promoteTo = Iterables.getOnlyElement(children);
                    if (!promoteTo.getName().equalsIgnoreCase(choice)) {
                        sender.sendMessage(Messages.format(ERROR_PROMOTE_CHOICE_NOT_FOUND, playerName, choice));
                        return;
                    }
                    if (!user.addParent(promoteTo) || !user.removeParent(playerOnlyGroup)) {
                        throw new AssertionError("A group was it's own parent? (" + promoteTo.getName() + ", " + playerOnlyGroup.getName() + ", " + CommandUtils.getPlayerName(playerName) + ")");
                    }
                    sender.sendMessage(Messages.format(SUCCESS_PLAYER_PROMOTE, playerName, playerOnlyGroup.getName(), promoteTo.getName()));
                } else {
                    for (PermissionGroup g : children) { // A choice and multiple options.
                        if (g.getName().equalsIgnoreCase(choice)) {
                            if (!g.getName().equalsIgnoreCase(choice)) {
                                sender.sendMessage(Messages.format(ERROR_PROMOTE_CHOICE_NOT_FOUND, playerName, choice));
                                return;
                            }
                            if (!user.addParent(g) || !user.removeParent(g)) {
                                throw new AssertionError("A group was it's own parent? (" + g.getName() + ", " + playerOnlyGroup.getName() + ", " + CommandUtils.getPlayerName(playerName) + ")");
                            }
                            sender.sendMessage(Messages.format(SUCCESS_PLAYER_PROMOTE, playerName, playerOnlyGroup.getName(), g.getName()));
                            return;
                        }
                    } // Otherwise we've failed.
                    if (choice == null) {
                        sender.sendMessage(Messages.format(ERROR_PROMOTE_SUBGROUPS));
                    } else {
                        sender.sendMessage(Messages.format(ERROR_PROMOTE_SUBGROUPS_CHOICE, choice));
                    }
                    for (PermissionGroup g : children) {
                        sender.sendMessage(Messages.format(ERROR_PROMOTE_SUBGROUP_VALUE, g.getName()));
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
            CommandUtils.loadPlayerGroups(plugin.getUserManager(), value, args[0], ret);
        }
        return ret;
    }
}
