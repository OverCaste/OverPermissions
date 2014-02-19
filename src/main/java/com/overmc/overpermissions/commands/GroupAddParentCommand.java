package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.*;

import org.bukkit.command.*;

import com.overmc.overpermissions.*;

// ./groupadd [group] [parent]
public class GroupAddParentCommand implements TabExecutor {
	private final OverPermissions plugin;

	public GroupAddParentCommand(OverPermissions plugin) {
		this.plugin = plugin;
	}

	public GroupAddParentCommand register( ) {
		PluginCommand command = plugin.getCommand("groupaddparent");
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
		if (args.length != 2) {
			sender.sendMessage(Messages.getUsage(command));
			return true;
		}
		Group group = plugin.getGroupManager().getGroup(args[0]);
		if (group == null) {
			sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, args[0]));
			return true;
		}
		Group parent = plugin.getGroupManager().getGroup(args[1]);
		if (parent == null) {
			sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, args[1]));
			return true;
		}
		for (Group g : parent.getAllParents()) {
			if (g.equals(group)) {
				sender.sendMessage(Messages.format(ERROR_PARENT_RECURSION, parent.getName(), group.getName()));
				return true;
			}
		}
		if (plugin.getSQLManager().addGroupParent(group.getId(), parent.getId())) {
			sender.sendMessage(Messages.format(SUCCESS_GROUP_ADD_PARENT, args[1], group.getName()));
			group.recalculateParents();
			parent.recalculateChildren();
		} else {
			sender.sendMessage(Messages.format(ERROR_PARENT_ALREADY_SET, args[1]));
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
			for (Group g : plugin.getGroupManager().getGroups()) {
				if (g.getName().toLowerCase().startsWith(value)) {
					ret.add(g.getName());
				}
			}
		} else if (index == 1) {
			for (Group g : plugin.getGroupManager().getGroups()) {
				if (g.getName().toLowerCase().startsWith(value)) {
					ret.add(g.getName());
				}
				ret.remove(args[0]);
			}
		}
		return ret;
	}
}
