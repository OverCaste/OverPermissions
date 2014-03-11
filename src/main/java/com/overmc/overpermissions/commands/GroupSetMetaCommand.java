package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;

import com.google.common.base.Joiner;
import com.overmc.overpermissions.Group;
import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;

// /groupsetmeta [group] [key] ((server:)world) [value|'clear']
public class GroupSetMetaCommand implements TabExecutor {
	private final OverPermissions plugin;

	public GroupSetMetaCommand(OverPermissions plugin) {
		this.plugin = plugin;
	}

	public GroupSetMetaCommand register( ) {
		PluginCommand command = this.plugin.getCommand("groupsetmeta");
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
		if (args.length < 3) {
			sender.sendMessage(Messages.getUsage(command));
			return true;
		}
		Group group = this.plugin.getGroupManager().getGroup(args[0]);
		if (group == null) {
			sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, args[0]));
			return true;
		}
		if ((args.length == 3) && "clear".equalsIgnoreCase(args[2])) {
			if (this.plugin.getSQLManager().removeGroupMeta(group.getId(), args[1])) {
				sender.sendMessage(Messages.format(SUCCESS_GROUP_META_CLEAR, args[1]));
				group.recalculateMeta();
			} else {
				sender.sendMessage(Messages.format(ERROR_UNKNOWN_META_KEY, args[1]));
			}
		} else {
			String value = Joiner.on(' ').join(Arrays.copyOfRange(args, 2, args.length));
			this.plugin.getSQLManager().setGroupMeta(group.getId(), args[1], value);
			sender.sendMessage(Messages.format(SUCCESS_GROUP_META_SET, args[1], value));
			group.recalculateMeta();
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
			for (Group g : this.plugin.getGroupManager().getGroups()) {
				if (g.getName().toLowerCase().startsWith(value)) {
					ret.add(g.getName());
				}
			}
		} else if (index == 1) {
			Group g = this.plugin.getGroupManager().getGroup(args[0]);
			if (g != null) {
				for (Map.Entry<String, String> meta : g.getAllMeta()) {
					ret.add(meta.getKey());
				}
				if (!ret.contains("prefix")) {
					ret.add("prefix");
				}
				if (!ret.contains("suffix")) {
					ret.add("suffix");
				}
			}
		} else if (index == 2) {
			ret.add("clear");
		}
		return ret;
	}
}
