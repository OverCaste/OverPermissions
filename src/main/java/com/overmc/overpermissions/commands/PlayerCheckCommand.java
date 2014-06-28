package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;
import com.overmc.overpermissions.PlayerPermissionData;

// ./playercheck [player] [permission] (world)
public class PlayerCheckCommand implements TabExecutor {
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
        if ((args.length < 2) || (args.length > 3)) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        plugin.getExecutor().execute(new Runnable() { // This is entirely safe because getSQLManager() should never change, and Player.sendMessage should be threadsafe.
            @Override
            public void run( ) {
                @SuppressWarnings("deprecation")
                Player p = Bukkit.getPlayerExact(args[0]);
                final PlayerPermissionData playerData = (p == null ? null : plugin.getPlayerPermissions(p));
                int playerId = (playerData == null) ? plugin.getUuidManager().getOrCreateSqlUser(args[0]) : playerData.getId();
                int worldId;
                if (args.length < 3) {
                    worldId = -1;
                } else {
                    if ("global".equalsIgnoreCase(args[2])) {
                        worldId = -1;
                    } else {
                        worldId = plugin.getSQLManager().getWorldId(args[2], false);
                        if (worldId < 0) {
                            sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, args[2]));
                            return;
                        }
                    }
                }
                String permission = args[1];
                if (permission.startsWith("+") || permission.startsWith("-")) {
                    permission = permission.substring(1);
                }
                List<String> message = (playerData == null ? plugin.getSQLManager().getPlayerNodeInfo(playerId, worldId, permission) : playerData.getNodeInfo(permission));
                boolean value = (playerData == null ? plugin.getSQLManager().checkPlayerPermissionExists(playerId, worldId, permission) : playerData.getPermission(permission));
                if (message.size() == 0) {
                    sender.sendMessage(Messages.format(Messages.ERROR_PLAYER_PERMISSION_NOT_SET, args[1]));
                } else {
                    sender.sendMessage(Messages.format(Messages.VALUE_OF_PERMISSION, permission, value));
                    sender.sendMessage(message.toArray(new String[message.size()]));
                }
            }
        });
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
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(value)) {
                    ret.add(p.getName());
                }
            }
        } else if (index == 1) {
            @SuppressWarnings("deprecation")
            Player p = plugin.getServer().getPlayerExact(args[0]);
            if (p != null) {
                for (String node : plugin.getPlayerPermissions(p).getNodes()) {
                    if (node.startsWith("-") || node.startsWith("+")) {
                        continue;
                    }
                    ret.add(node);
                }
            } else {
                int worldId = plugin.getSQLManager().getWorldId(args[0]);
                for (String node : plugin.getSQLManager().getTotalPlayerNodes(worldId, -1)) {
                    if (node.startsWith("-") || node.startsWith("+")) {
                        continue;
                    }
                    ret.add(node);
                }
            }
        } else if (index == 2) {
            for (World w : plugin.getServer().getWorlds()) {
                if (w.getName().toLowerCase().startsWith(value)) {
                    ret.add(w.getName());
                }
                ret.add("global");
            }
        }
        return ret;
    }
}
