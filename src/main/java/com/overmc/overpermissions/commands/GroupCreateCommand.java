package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.*;

import org.bukkit.*;
import org.bukkit.command.*;

import com.overmc.overpermissions.*;

// ./groupcreate [group] [priority] (world)
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
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission(command.getPermission())) {
			sender.sendMessage(ERROR_NO_PERMISSION);
			return true;
		}
		if ((args.length < 2) || (args.length > 3)) {
			sender.sendMessage(Messages.getUsage(command));
			return true;
		}
		World world;
		if (args.length < 3) {
			world = null;
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
		int worldId = plugin.getSQLManager().getWorldId(world);
		if (plugin.getGroupManager().getGroup(args[0]) != null) {
			sender.sendMessage(Messages.format(ERROR_GROUP_ALREADY_EXISTS, plugin.getGroupManager().getGroup(args[0]).getName()));
			return true;
		}
		int priority = 0;
		try {
			priority = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			sender.sendMessage(Messages.format(ERROR_INVALID_INTEGER, args[1]));
			return true;
		}
		plugin.getSQLManager().setGroup(args[0], priority, worldId);
		sender.sendMessage(Messages.format(SUCCESS_GROUP_CREATE, args[0], priority, (world == null ? "global" : world.getName())));
		plugin.getGroupManager().recalculateGroups();
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
		if (index == 2) {
			for (org.bukkit.World w : plugin.getServer().getWorlds()) {
				if (w.getName().toLowerCase().startsWith(value)) {
					ret.add(w.getName());
				}
			}
			ret.add("global");
		}
		return ret;
	}
}
