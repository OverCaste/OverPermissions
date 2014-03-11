package com.overmc.overpermissions;

import static org.bukkit.ChatColor.*;

import org.bukkit.command.Command;

public class Messages {

	/* Error messages */
	public static final String ERROR_NO_PERMISSION = RED + "You don't have permission to do that!";
	public static final String ERROR_GROUP_PERMISSION_ALREADY_SET = RED + "That group already has permission %s!";
	public static final String ERROR_PLAYER_PERMISSION_ALREADY_SET = RED + "That player already has permission %s!";
	public static final String ERROR_PARENT_ALREADY_SET = RED + "That group already has parent %s!";
	public static final String ERROR_PARENT_NOT_SET = RED + "That group doesn't have parent %s!";
	public static final String ERROR_PARENT_RECURSION = RED + "Group %s has group %s as a parent!";
	public static final String ERROR_GROUP_PERMISSION_NOT_SET = RED + "That group doesn't have permission %s!";
	public static final String ERROR_GROUP_NO_CHILDREN = RED + "Group %s doesn't have any children!";
	public static final String ERROR_PLAYER_ALREADY_IN_GROUP = RED + "Player %s is already in group %s!";
	public static final String ERROR_PLAYER_PERMISSION_NOT_SET = RED + "That player doesn't have permission %s!";
	public static final String ERROR_INVALID_WORLD = RED + "'%s' isn't a valid world or 'global.'";
	public static final String ERROR_GROUP_NOT_FOUND = RED + "Couldn't find a group by the name of '%s'.";
	public static final String ERROR_DELETE_DEFAULT_GROUP = RED + "You can't delete group %s! It is the default group!";
	public static final String ERROR_INVALID_INTEGER = RED + "'%s' isn't a valid number!";
	public static final String ERROR_UNKNOWN_META_KEY = RED + "%s isn't a set metadata key!";
	public static final String ERROR_UNKNOWN = RED + "An unknown error has occurred!";
	public static final String ERROR_PROMOTE_PLAYER_MULTIPLE_GROUPS = RED + "Player %s is in multiple groups! Please specify the group to promote them to.";
	public static final String ERROR_PLAYER_NOT_IN_GROUP_WORLD = RED + "Player %s is not in any groups in world %s!";
	public static final String ERROR_GROUP_ALREADY_EXISTS = RED + "A group by the name of %s already exists!";

	/* Console error messages */
	public static final String ERROR_SQL_NOT_CONNECTED = "Failed to connect to the MySQL database at \"%s.\" Please ensure it is turned on.";

	/* Success messages */
	public static final String SUCCESS_GROUP_CREATE = GREEN + "Successfully created group \"%s\" with priority %s.";
	public static final String SUCCESS_GROUP_DELETE = GREEN + "Successfully deleted group \"%s.\"";
	public static final String SUCCESS_GROUP_META_SET = GREEN + "Successfully set metadata key %s to value %s.";
	public static final String SUCCESS_GROUP_META_CLEAR = GREEN + "Successfully unset metadata at key %s.";
	public static final String SUCCESS_GROUP_ADD_PARENT = GREEN + "Successfully added parent %s to group %s.";
	public static final String SUCCESS_GROUP_REMOVE_PARENT = GREEN + "Successfully removed parent %s from group %s.";
	public static final String SUCCESS_PLAYER_SET_GROUP = GREEN + "Successfully set player %s's group to %s.";
	public static final String SUCCESS_PLAYER_ADD_GROUP = GREEN + "Successfully added player %s to group %s.";
	public static final String SUCCESS_GROUP_ADD = GREEN + "Successfully added permission %s to group %s.";
	public static final String SUCCESS_GROUP_REMOVE = GREEN + "Successfully removed permission %s from group %s.";
	public static final String SUCCESS_PLAYER_ADD = GREEN + "Successfully added permission %s to player %s.";
	public static final String SUCCESS_PLAYER_ADD_WORLD = GREEN + "Successfully added permission %s to player %s in world %s.";
	public static final String SUCCESS_PLAYER_ADDTEMP = GREEN + "Successfully added permission %s to player %s for %s seconds.";
	public static final String SUCCESS_PLAYER_ADDTEMP_WORLD = GREEN + "Successfully added permission %s to player %s in world %s for %s seconds.";
	public static final String SUCCESS_PLAYER_REMOVE = GREEN + "Successfully removed permission %s from player %s.";
	public static final String SUCCESS_PLAYER_REMOVE_WORLD = GREEN + "Successfully removed permission %s from player %s in world %s.";
	public static final String SUCCESS_PLAYER_PROMOTE = GREEN + "Successfully promoted player %s from group %s to group %s.";

	/* Info messages */
	public static final String GROUPS_WITH_NODE = AQUA + "Values set by groups with that node:";
	public static final String GROUP_NODE_VALUE = AQUA + "%s";
	public static final String PLAYER_NODES = AQUA + "Values added by global and world player permissions:";
	public static final String PLAYER_NODE_VALUE = AQUA + "%s";
	public static final String PLAYER_TEMP_NODES = AQUA + "Values added by temporary player permissions:";
	public static final String PLAYER_TEMP_NODE_VALUE = AQUA + "%s";
	public static final String PLAYER_TRANSIENT_NODES = AQUA + "Values added by transient player permissions:";
	public static final String PLAYER_TRANSIENT_NODE_VALUE = AQUA + "%s";
	public static final String VALUE_OF_PERMISSION = AQUA + "The value of permission " + GOLD + "%s" + AQUA + " is: " + GOLD + "%s" + AQUA + ".";
	public static final String PROMOTE_SUBGROUPS = AQUA + "Groups that player can be promoted to:";
	public static final String PROMOTE_SUBGROUP_VALUE = AQUA + "%s";
	public static final String PLUGIN_INFO_MESSAGE = AQUA + "%s v%s. Created by %s.";

	/* Utility messages */
	public static final String MESSAGE_BARS = AQUA + "-----------------------------------------------------";

	public static String format(String message, Object... values) {
		return String.format(message, values);
	}

	public static String getUsage(Command command) {
		return translateAlternateColorCodes('&', command.getUsage());
	}
}
