package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.*;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.*;
import com.overmc.overpermissions.events.*;

// ./playeradd [player] [permission] (world)
public class PlayerAddCommand implements TabExecutor {
	private final OverPermissions plugin;

	public PlayerAddCommand(OverPermissions plugin) {
		this.plugin = plugin;
	}

	public PlayerAddCommand register( ) {
		PluginCommand command = plugin.getCommand("playeradd");
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
		if ((args.length < 2) || (args.length > 3)) {
			sender.sendMessage(Messages.getUsage(command));
			return true;
		}
		Player p = plugin.getServer().getPlayerExact(args[0]);
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
					return true;
				}
			}
		}
		PlayerPermissionAddEvent e;
		if (sender instanceof Player) {
			e = new PlayerPermissionAddByPlayerEvent(args[0], (world == null) ? null : world.getName(), args[1], (Player) sender);
		} else {
			e = new PlayerPermissionAddEvent(args[0], (world == null) ? null : world.getName(), args[1], PermissionChangeCause.CONSOLE);
		}
		plugin.getServer().getPluginManager().callEvent(e);
		if (e.isCancelled()) {
			return true;
		}
		int playerId = (p == null) ? plugin.getSQLManager().getPlayerId(args[0], true) : plugin.getPlayerPermissions(p).getId();
		boolean success;
		if (world == null) {
			success = plugin.getSQLManager().addGlobalPlayerPermission(playerId, args[1]);
		} else {
			int worldId = plugin.getSQLManager().getWorldId(world);
			success = plugin.getSQLManager().addPlayerPermission(playerId, worldId, args[1]);
		}
		if (success) {
			if (world == null) {
				sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADD, args[1], args[0]));
			} else {
				sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADD_WORLD, args[1], args[0], world.getName()));
			}
			if (p != null) {
				plugin.getPlayerPermissions(p).recalculatePermissions();
			}
		} else {
			sender.sendMessage(Messages.format(ERROR_PLAYER_PERMISSION_ALREADY_SET, args[1]));
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
