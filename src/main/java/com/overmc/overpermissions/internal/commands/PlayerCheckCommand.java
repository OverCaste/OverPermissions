package com.overmc.overpermissions.internal.commands;

import static com.overmc.overpermissions.internal.Messages.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.overmc.nodedisplayapi.BoxLargeAndSmallCharset;
import com.overmc.nodedisplayapi.ElementBox;
import com.overmc.nodedisplayapi.ElementBoxNode;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.internal.Messages;
import com.overmc.overpermissions.internal.OverPermissions;

// ./playercheck [player] [permission] (world)
public class PlayerCheckCommand implements TabExecutor { // TODO cleanup, add wildcard support
    private final OverPermissions plugin;

    public PlayerCheckCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerCheckCommand register( ) {
        PluginCommand command = plugin.getCommand("playercheck");
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
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        String playerName = args[0];
        String permission = args[1];
        String world = (args.length >= 3 ? args[2].toLowerCase() : null);
        boolean global = (world == null || "global".equals(world));
        if (!plugin.getUserManager().canUserExist(playerName)) {
            sender.sendMessage(Messages.format(ERROR_PLAYER_INVALID_NAME, playerName));
            return true;
        }
        boolean groupPermissionExists = false;
        boolean userPermissionExists = false;
        @SuppressWarnings("deprecation")
        Player player = Bukkit.getPlayerExact(playerName);

        List<ElementBoxNode> groupInformationNodes = new ArrayList<>();
        PermissionUser user = plugin.getUserManager().getPermissionUser(playerName);
        List<String> listBuffer = new ArrayList<>(); // As to not reuse an array list many times when unnecessary.
        for (PermissionGroup group : user.getAllParents()) {
            listBuffer.clear();
            Set<String> effectivePermissions = new HashSet<String>();
            effectivePermissions.addAll(group.getPermissionNodes().getGlobalNodes());
            if (!global) {
                effectivePermissions.addAll(group.getPermissionNodes().getWorldNodes().get(world));
            }
            for (String node : effectivePermissions) {
                String baseNode = node.startsWith("+") || node.startsWith("-") ? node.substring(1) : node;
                if (baseNode.equalsIgnoreCase(permission)) {
                    if (node.startsWith("+")) {
                        listBuffer.add("+ added");
                    } else if (node.startsWith("-")) {
                        listBuffer.add("- force-removed");
                    } else {
                        listBuffer.add("+ added");
                    }
                }
            }
            if (!listBuffer.isEmpty()) {
                groupInformationNodes.add(new ElementBoxNode(group.getName() + ":", null, listBuffer));
                groupPermissionExists = true;
            }
        }
        listBuffer.clear();
        Set<String> effectivePermissions = new HashSet<String>();
        effectivePermissions.addAll(user.getPermissionNodes().getGlobalNodes());
        if (!global) {
            effectivePermissions.addAll(user.getPermissionNodes().getWorldNodes().get(world));
        }
        for (String node : effectivePermissions) {
            String baseNode = node.startsWith("+") || node.startsWith("-") ? node.substring(1) : node;
            if (baseNode.equalsIgnoreCase(permission)) {
                if (node.startsWith("+")) {
                    listBuffer.add("+ added");
                } else if (node.startsWith("-")) {
                    listBuffer.add("- force-removed");
                } else {
                    listBuffer.add("+ added");
                }
                userPermissionExists = true;
            }
        }

        List<ElementBoxNode> nodes = new ArrayList<>();
        if (global ? user.hasGlobalPermission(permission) : user.hasPermission(permission, world)) {
            nodes.add(new ElementBoxNode("Permission value: " + (global ? user.getGlobalPermission(permission) : user.getPermission(permission, world)), null, null));
        }
        if (player != null) {
            nodes.add(new ElementBoxNode("Bukkit reports that permission as: " + player.hasPermission(permission), null, null));
        }
        if (groupPermissionExists) {
            nodes.add(new ElementBoxNode("Group information:", groupInformationNodes, null));
        }
        if (userPermissionExists) {
            nodes.add(new ElementBoxNode("User information:", null, listBuffer));
        }
        if (nodes.isEmpty()) {
            if (global) {
                sender.sendMessage(Messages.format(ERROR_PLAYER_PERMISSION_NOT_SET_GLOBAL, permission));
            } else {
                sender.sendMessage(Messages.format(ERROR_PLAYER_PERMISSION_NOT_SET_WORLD, permission, world));
            }
            return true;
        }

        ElementBox box = new ElementBox(16, new BoxLargeAndSmallCharset(), nodes, Arrays.asList("Permission '" + permission + "'"));
        for (String s : box.write()) {
            int i = 0;
            while (s.charAt(i) == ' ') {
                i++;
            }
            if (s.startsWith("      \u2514")) { // Replace the 6 space separator with a 5 space separator.
                s = s.replace("      \u2514", "     \u2514"); // The hackiest solution ever, but it's not my fault minecraft's font isn't monospaced.
            }
            sender.sendMessage(Messages.format(Messages.PLAYER_NODE_VALUE, s));
        }
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
            CommandUtils.loadBasePlayerPermissionNodes(plugin.getUserManager(), value, args[0], ret);
        } else if (index == 2) {
            CommandUtils.loadWorlds(value, ret);
            CommandUtils.loadGlobalWorldConstant(value, ret);
        }
        return ret;
    }
}
