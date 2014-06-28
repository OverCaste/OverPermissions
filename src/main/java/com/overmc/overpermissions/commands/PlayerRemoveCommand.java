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
import com.overmc.overpermissions.PermissionChangeCause;
import com.overmc.overpermissions.events.PlayerPermissionRemoveByPlayerEvent;
import com.overmc.overpermissions.events.PlayerPermissionRemoveEvent;

// ./playerremove [player] [permission] (world)
public class PlayerRemoveCommand implements TabExecutor {
    private final OverPermissions plugin;

    public PlayerRemoveCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public PlayerRemoveCommand register( ) {
        PluginCommand command = plugin.getCommand("playerremove");
        command.setExecutor(this);
        command.setTabCompleter(this);
        return this;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
        if (!sender.hasPermission("overpermissions.playerremove")) {
            sender.sendMessage(ERROR_NO_PERMISSION);
            return true;
        }
        if ((args.length < 2) || (args.length > 3)) {
            sender.sendMessage(Messages.getUsage(command));
            return true;
        }
        plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
                @SuppressWarnings("deprecation")
                Player p = plugin.getServer().getPlayerExact(args[1]);
                World world;
                if (args.length < 3) {
                    if (sender instanceof Player) {
                        world = ((Player) sender).getWorld();
                    } else {
                        world = null;
                    }
                } else {
                    if ("global".equalsIgnoreCase(args[2])) {
                        world = null;
                    } else {
                        world = Bukkit.getWorld(args[2]);
                        if (world == null) {
                            sender.sendMessage(Messages.format(ERROR_INVALID_WORLD, args[2]));
                            return;
                        }
                    }
                }
                PlayerPermissionRemoveEvent e;
                if (sender instanceof Player) {
                    e = new PlayerPermissionRemoveByPlayerEvent(args[0], (world == null) ? null : world.getName(), args[1], (Player) sender);
                } else {
                    e = new PlayerPermissionRemoveEvent(args[0], (world == null) ? null : world.getName(), args[1], PermissionChangeCause.CONSOLE);
                }
                plugin.getServer().getPluginManager().callEvent(e);
                if (e.isCancelled()) {
                    return;
                }
                int worldId = (world == null ? -1 : plugin.getSQLManager().getWorldId(world));
                int playerId = plugin.getUuidManager().getOrCreateSqlUser(args[0]);
                if (playerId < 0) {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_LOOKUP_FAILED, args[0]));
                    return;
                }
                boolean success = (worldId < 0 ? plugin.getSQLManager().removeGlobalPlayerPermission(playerId, args[1]) : plugin.getSQLManager().removePlayerPermission(playerId, worldId, args[1]));
                if ((playerId >= 0) && success) {
                    if (world == null) {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_REMOVE, args[1], args[0]));
                    } else {
                        sender.sendMessage(Messages.format(SUCCESS_PLAYER_REMOVE_WORLD, args[1], args[0], world.getName()));
                    }
                    if (p != null) {
                        plugin.getPlayerPermissions(p).recalculatePermissions();
                    }
                } else {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_PERMISSION_NOT_SET, args[1]));
                }
            }
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> ret = new ArrayList<String>();
        int index = args.length - 1;
        String value = args[index].toLowerCase();
        if (index == 0) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(value)) {
                    ret.add(p.getName());
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
