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

// ./playeraddtemp [player] [permission] [time] (world)
public class PlayerAddTempCommand implements TabExecutor {
    private final OverPermissions plugin;

    public PlayerAddTempCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerAddTempCommand register( ) {
        PluginCommand command = plugin.getCommand("playeraddtemp");
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
        if ((args.length < 3) || (args.length > 4)) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                World world;
                if (args.length < 4) {
                    if (sender instanceof Player) {
                        world = ((Player) sender).getWorld();
                    } else {
                        world = null;
                    }
                } else {
                    if ("global".equalsIgnoreCase(args[3])) {
                        world = null;
                    } else {
                        world = Bukkit.getWorld(args[3]);
                        if (world == null) {
                            sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, args[3]));
                            return;
                        }
                    }
                }
                int worldId = (world == null ? -1 : plugin.getSQLManager().getWorldId(world));
                int playerId = plugin.getUuidManager().getOrCreateSqlUser(args[0]);
                if (playerId < 0) {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_LOOKUP_FAILED, args[0]));
                    return;
                }
                int time;
                try {
                    time = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Messages.format(ERROR_INVALID_INTEGER, args[2]));
                    return;
                }
                if (plugin.getTempManager().registerTemporaryPlayerPermission(playerId, worldId, time, args[1])) {
                    if (world == null) {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADDTEMP, args[1], args[0], time));
                    } else {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADDTEMP_WORLD, args[1], args[0], world.getName(), time));
                    }
                } else {
                    sender.sendMessage(Messages.format(ERROR_GROUP_PERMISSION_ALREADY_SET, args[1]));
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
        } else if (index == 3) {
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
