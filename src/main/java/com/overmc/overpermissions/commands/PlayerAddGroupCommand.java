package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.*;

// ./playeraddgroup [player] [group]
public class PlayerAddGroupCommand implements TabExecutor {
	private final OverPermissions plugin;

	public PlayerAddGroupCommand(OverPermissions plugin) {
		this.plugin = plugin;
	}

	public PlayerAddGroupCommand register( ) {
		PluginCommand command = plugin.getCommand("playeraddgroup");
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
		if ((args.length < 2) || (args.length > 2)) {
			sender.sendMessage(Messages.getUsage(command));
			return true;
		}
		String victim = args[0];
		int groupId = plugin.getSQLManager().getGroupId(args[1]);
		if (groupId < 0) {
			sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, args[1]));
			return true;
		}
		int victimId = plugin.getSQLManager().getPlayerId(victim, true);
		if (plugin.getSQLManager().addPlayerGroup(victimId, groupId)) {
			sender.sendMessage(Messages.format(SUCCESS_PLAYER_ADD_GROUP, victim, args[1]));
			Player p = Bukkit.getPlayerExact(victim);
			if (p != null) {
				plugin.getPlayerPermissions(p).recalculateGroups();
			}
		} else {
			sender.sendMessage(Messages.format(ERROR_PLAYER_ALREADY_IN_GROUP, victim, args[1]));
		}
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
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.getName().toLowerCase().startsWith(value)) {
					ret.add(player.getName());
				}
			}
		} else if (index == 1) {
			for (Group g : plugin.getGroupManager().getGroups()) {
				ret.add(g.getName());
			}
		}
		return ret;
	}
}
