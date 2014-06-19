package com.overmc.overpermissions.commands;

import static com.overmc.overpermissions.Messages.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.google.common.collect.Iterators;
import com.overmc.overpermissions.Group;
import com.overmc.overpermissions.Messages;
import com.overmc.overpermissions.OverPermissions;

// ./playerpromote [player] (choice) (world)
public class PlayerPromoteCommand implements TabExecutor {
	private final OverPermissions plugin;

	public PlayerPromoteCommand(OverPermissions plugin) {
		this.plugin = plugin;
	}

	public PlayerPromoteCommand register( ) {
		PluginCommand command = plugin.getCommand("playerpromote");
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
		if ((args.length < 1) || (args.length > 3)) {
			sender.sendMessage(Messages.getUsage(command));
			return true;
		}

		plugin.getExecutor().submit(new Runnable() {
            @Override
            public void run( ) {
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
                int worldId = (world == null ? -1 : plugin.getSQLManager().getWorldId(world));
                Player player = Bukkit.getPlayerExact(args[0]);
                int playerId = plugin.getUuidManager().getOrCreateSqlUser(args[0]);
                if(playerId < 0) {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_LOOKUP_FAILED, args[0]));
                    return;
                }
                String choice = (args.length < 2 ? null : args[1].toLowerCase());
                Group playerGroup = null;
                for (int groupId : plugin.getSQLManager().getPlayerGroups(playerId)) {
                    Group groupi = plugin.getGroupManager().getGroup(groupId);
                    if (((groupi.getWorldId() == -1) || (groupi.getWorldId() == worldId)) && ((choice == null) || (groupi.getName().toLowerCase().startsWith(choice)))) {
                        if (playerGroup != null) {
                            sender.sendMessage(Messages.format(ERROR_PROMOTE_PLAYER_MULTIPLE_GROUPS, args[0]));
                            return;
                        } else {
                            playerGroup = groupi;
                        }
                    }
                }
                if (playerGroup != null) {
                    Collection<Group> children = playerGroup.getChildren();
                    if (children.size() == 0) {
                        sender.sendMessage(Messages.format(ERROR_GROUP_NO_CHILDREN, playerGroup.getName()));
                    } else if (children.size() == 1) {
                        Group promoteTo = Iterators.getOnlyElement(children.iterator());
                        if (plugin.getSQLManager().addPlayerGroup(playerId, promoteTo.getId()) && plugin.getSQLManager().removePlayerGroup(playerId, playerGroup.getId())) {
                            sender.sendMessage(Messages.format(SUCCESS_PLAYER_PROMOTE, args[0], playerGroup.getName(), promoteTo.getName()));
                            if (player != null) {
                                plugin.getPlayerPermissions(player).recalculateGroups();
                            }
                            return;
                        }
                    } else {
                        sender.sendMessage(Messages.format(PROMOTE_SUBGROUPS));
                        for (Group g : children) {
                            sender.sendMessage(Messages.format(PROMOTE_SUBGROUP_VALUE, g.getName()));
                        }
                    }
                } else {
                    sender.sendMessage(Messages.format(ERROR_PLAYER_NOT_IN_GROUP_WORLD, args[0], (world == null ? "global" : world.getName())));
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
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.getName().toLowerCase().startsWith(value)) {
					ret.add(p.getName());
				}
			}
		} else if (index == 1) {
			int playerId = plugin.getSQLManager().getPlayerId(args[0]);
			if (playerId < 0) {
				return ret;
			}
			for (int groupId : plugin.getSQLManager().getPlayerGroups(playerId)) {
				Group group = plugin.getGroupManager().getGroup(groupId);
				for (Group other : group.getChildren()) {
					String name = other.getName();
					if (name.toLowerCase().startsWith(value) && !ret.contains(name)) {
						ret.add(name);
					}
				}
			}
		} else if (index == 2) {
			for (World world : Bukkit.getWorlds()) {
				if (world.getName().toLowerCase().startsWith(value)) {
					ret.add(world.getName());
				}
			}
			ret.add("global");
		}
		return ret;
	}
}
