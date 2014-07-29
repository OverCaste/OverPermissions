package com.overmc.overpermissions.internal.commands;

import static com.overmc.overpermissions.internal.Messages.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import com.google.common.base.Joiner;
import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.internal.Messages;
import com.overmc.overpermissions.internal.OverPermissions;

// ./overpermissions ['debug'|'info']
public class OverPermissionsCommand implements CommandExecutor {
    private final OverPermissions plugin;

    public OverPermissionsCommand(OverPermissions plugin) {
        this.plugin = plugin;
    }

    public OverPermissionsCommand register( ) {
        PluginCommand command = plugin.getCommand("overpermissions");
        command.setExecutor(this);
        return this;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(command.getPermission())) {
            sender.sendMessage(ERROR_NO_PERMISSION);
            return true;
        }

        if ((args.length == 0) || ((args.length == 1) && "info".equalsIgnoreCase(args[0]))) {
            sender.sendMessage(Messages.format(PLUGIN_INFO_MESSAGE, plugin.getName(), plugin.getDescription().getVersion(), Joiner.on(',').join(plugin.getDescription().getAuthors())));
            return true;
        }
        if ("debug".equalsIgnoreCase(args[0])) {
            if (args.length == 1) {
                sender.sendMessage("debug commands: ");
                sender.sendMessage("/overperms debug player [player]");
                return true;
            }
            else if ((args.length == 3)) {
                if ("player".equalsIgnoreCase(args[1])) {
                    String playername = args[2];
                    if (plugin.getUserManager().doesUserExist(playername)) {
                        sender.sendMessage("Player not found.");
                        return true;
                    }
                    PermissionUser user = plugin.getUserManager().getPermissionUser(playername);
                    sender.sendMessage("Player effective groups: " + Joiner.on(' ').join(user.getAllParents()));
                    sender.sendMessage("Player actual groups: " + Joiner.on(' ').join(user.getParents()));
                    return true;
                }
            }
        }
        sender.sendMessage(Messages.getUsage(command));
        return true;
    }
}
