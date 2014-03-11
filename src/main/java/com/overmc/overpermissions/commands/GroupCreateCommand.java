package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;

import com.overmc.overpermissions.Group;
import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;

// /groupcreate [group] [priority] [parent]
public class GroupCreateCommand implements TabExecutor {
	private final OverPermissions plugin;

	public GroupCreateCommand(OverPermissions plugin) {
		this.plugin = plugin;
	}

	public GroupCreateCommand register( ) {
		PluginCommand command = this.plugin.getCommand("groupcreate");
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
		if (this.plugin.getGroupManager().getGroup(args[0]) != null) {
			sender.sendMessage(Messages.format(ERROR_GROUP_ALREADY_EXISTS, this.plugin.getGroupManager().getGroup(args[0]).getName()));
			return true;
		}
		final int priority;
		try {
			priority = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			sender.sendMessage(Messages.format(ERROR_INVALID_INTEGER, args[1]));
			return true;
		}
		final Group parent;
		if (args.length == 3) {
			parent = this.plugin.getGroupManager().getGroup(args[2]);
			if (parent == null) {
				sender.sendMessage(Messages.format(ERROR_GROUP_NOT_FOUND, args[2]));
				return true;
			}
		} else {
			parent = null;
		}
		this.plugin.getExecutor().execute(new Runnable() {
			@Override
			public void run( ) {
				GroupCreateCommand.this.plugin.getSQLManager().createGroup(args[0], priority);
				if (parent != null) {
					int groupId = GroupCreateCommand.this.plugin.getSQLManager().getGroupId(args[0]);
					GroupCreateCommand.this.plugin.getSQLManager().addGroupParent(groupId, parent.getId());
				}
				sender.sendMessage(Messages.format(SUCCESS_GROUP_CREATE, args[0], priority));
				GroupCreateCommand.this.plugin.getGroupManager().recalculateGroups();
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
		if (index == 2) {
			for (Group g : this.plugin.getGroupManager().getGroups()) {
				if (g.getName().toLowerCase().startsWith(value)) {
					ret.add(g.getName());
				}
			}
		}
		return ret;
	}
}
