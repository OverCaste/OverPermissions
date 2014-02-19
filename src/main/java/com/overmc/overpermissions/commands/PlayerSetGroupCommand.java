package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.overmc.overpermissions.Group;
import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;
import com.overmc.overpermissions.events.PlayerGroupChangeEvent;

// ./groupset [player] [group]
public class PlayerSetGroupCommand implements TabExecutor {
	private final OverPermissions plugin;

	public PlayerSetGroupCommand(OverPermissions plugin) {
		this.plugin = plugin;
	}

	public PlayerSetGroupCommand register( ) {
		PluginCommand command = this.plugin.getCommand("playersetgroup");
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
		int group = this.plugin.getSQLManager().getGroupId(args[1]);
		if (group <= 0) {
			sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, args[1]));
			return true;
		}
		int victimId = this.plugin.getSQLManager().getPlayerId(victim, true);
		PlayerGroupChangeEvent event = new PlayerGroupChangeEvent(victim, this.plugin.getGroupManager().getGroup(group).getName());
		this.plugin.getServer().getPluginManager().callEvent(event);
		if (event.isEnabled()) {
			this.plugin.getSQLManager().setPlayerGroup(victimId, group);
			Player p = Bukkit.getPlayerExact(victim);
			if (p != null) {
				this.plugin.getPlayerPermissions(p).recalculateGroups();
			}
			sender.sendMessage(Messages.format(SUCCESS_PLAYER_SET_GROUP, victim, args[1]));
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
			for (Group g : this.plugin.getGroupManager().getGroups()) {
				if (g.getName().toLowerCase().startsWith(value)) {
					ret.add(g.getName());
				}
			}
		}
		return ret;
	}
}
