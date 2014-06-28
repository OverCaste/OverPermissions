package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.google.common.base.Joiner;
import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;

// ./playersetmeta [player] [key] (world) [value]
public class PlayerSetMetaCommand implements TabExecutor {
    private final OverPermissions plugin;

    public PlayerSetMetaCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerSetMetaCommand register( ) {
        PluginCommand command = plugin.getCommand("playersetmeta");
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
        if (args.length < 3) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                @SuppressWarnings("deprecation")
                Player p = plugin.getServer().getPlayerExact(args[0]);
                int playerId;
                if (p == null) {
                    playerId = plugin.getUuidManager().getOrCreateSqlUser(args[0]);
                    if (playerId < 0) {
                        sender.sendMessage(Messages.format(ERROR_PLAYER_LOOKUP_FAILED, args[0]));
                        return;
                    }
                } else {
                    playerId = plugin.getPlayerPermissions(p).getId();
                }
                World world = null;
                boolean worldValid = false;
                if (args.length > 3) {
                    if ("global".equalsIgnoreCase(args[3])) {
                        world = null;
                        worldValid = true;
                    } else {
                        world = Bukkit.getWorld(args[3]);
                        if (world == null) {
                            if (sender instanceof Player) {
                                world = ((Player) sender).getWorld();
                            }
                        } else {
                            worldValid = true;
                        }
                    }
                } else {
                    if (sender instanceof Player) {
                        world = ((Player) sender).getWorld();
                    }
                }

                String meta = (Joiner.on(' ').join(Arrays.copyOfRange(args, (worldValid ? 3 : 2), args.length)));
                int worldId = (world == null ? -1 : plugin.getSQLManager().getWorldId(world));
                if ("clear".equalsIgnoreCase(meta)) {
                    if (plugin.getSQLManager().delPlayerMeta(playerId, worldId, args[1])) {
                        sender.sendMessage(Messages.format(SUCCESS_GROUP_META_CLEAR, args[1]));
                        if (p != null) {
                            plugin.getPlayerPermissions(p).recalculateMeta();
                        }
                    } else {
                        sender.sendMessage(Messages.format(ERROR_UNKNOWN_META_KEY, args[1]));
                    }
                } else {
                    plugin.getSQLManager().setPlayerMeta(playerId, worldId, args[1], meta);
                    sender.sendMessage(Messages.format(SUCCESS_GROUP_META_SET, args[1], meta));
                    if (p != null) {
                        plugin.getPlayerPermissions(p).recalculateMeta();
                    }
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
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(value)) {
                    ret.add(p.getName());
                }
            }
        } else if (index == 1) {
            int playerId = plugin.getSQLManager().getPlayerId(args[0]);
            for (String s : plugin.getSQLManager().getPlayerMeta(playerId, -1).values()) {
                ret.add(s);
            }
            if (!ret.contains("prefix")) {
                ret.add("prefix");
            }
            if (!ret.contains("suffix")) {
                ret.add("suffix");
            }
        } else if (index == 2) {
            for (World w : plugin.getServer().getWorlds()) {
                if (w.getName().toLowerCase().startsWith(value)) {
                    ret.add(w.getName());
                }
                ret.add("global");
                ret.add("clear");
            }
        } else if (index == 3) {
            ret.add("clear");
        }
        return ret;
    }
}
