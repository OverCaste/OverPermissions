package com.overmc.overpermissions.internal;

import static org.bukkit.ChatColor.*;

import org.bukkit.command.Command;

public final class Messages {

    private Messages( ) {
        // No instantiation.
    }

    /* Error messages */
    public static final String ERROR_NO_PERMISSION = RED + "You don't have permission to do that!";
    public static final String ERROR_GROUP_PERMISSION_ALREADY_SET_GLOBAL = RED + "That group already has permission %s!";
    public static final String ERROR_GROUP_PERMISSION_ALREADY_SET_WORLD = RED + "That group already has permission %s in world %s!";
    public static final String ERROR_GROUP_PERMISSION_NOT_SET_GLOBAL = RED + "That group doesn't have permission %s!";
    public static final String ERROR_GROUP_PERMISSION_NOT_SET_WORLD = RED + "That group doesn't have permission %s in world %s!";
    public static final String ERROR_PLAYER_PERMISSION_ALREADY_SET_GLOBAL = RED + "That player already has permission %s!";
    public static final String ERROR_PLAYER_PERMISSION_ALREADY_SET_WORLD = RED + "That player already has permission %s in world %s!";
    public static final String ERROR_PLAYER_PERMISSION_NOT_SET_GLOBAL = RED + "That player doesn't have permission %s!";
    public static final String ERROR_PLAYER_PERMISSION_NOT_SET_WORLD = RED + "That player doesn't have permission %s in world %s!";
    public static final String ERROR_PARENT_ALREADY_SET = RED + "That group already has parent %s!";
    public static final String ERROR_PARENT_NOT_SET = RED + "That group doesn't have parent %s!";
    public static final String ERROR_PARENT_RECURSION = RED + "Group %s has group %s as a parent!";
    public static final String ERROR_GROUP_NO_CHILDREN = RED + "Group %s doesn't have any children!";
    public static final String ERROR_PLAYER_ALREADY_IN_GROUP = RED + "Player %s is already in group %s!";
    public static final String ERROR_PLAYER_LOOKUP_FAILED = RED + "A player by the name of %s doesn't exist.";
    public static final String ERROR_PLAYER_INVALID_NAME = RED + "A player can't be named %s!";
    public static final String ERROR_INVALID_WORLD = RED + "'%s' isn't a valid world or 'global.'";
    public static final String ERROR_GROUP_NOT_FOUND = RED + "Couldn't find a group by the name of '%s'.";
    public static final String ERROR_DELETE_DEFAULT_GROUP = RED + "You can't delete group %s! It is the default group!";
    public static final String ERROR_DELETE_GROUP_HAS_PARENTS = RED + "You can't delete group %s! It has one or more parents: (%s)";
    public static final String ERROR_DELETE_GROUP_HAS_CHILDREN = RED + "You can't delete group %s! One or more groups have it as a parent: (%s)";
    public static final String ERROR_INVALID_INTEGER = RED + "'%s' isn't a valid number!";
    public static final String ERROR_INVALID_TIME = RED + "'%s' isn't a valid date! Use this format: 12.5d5h15m";
    public static final String ERROR_PLAYER_UNKNOWN_META_KEY_GLOBAL = RED + "%s isn't a set metadata key for player %s!";
    public static final String ERROR_PLAYER_UNKNOWN_META_KEY_WORLD = RED + "%s isn't a set metadata key for player %s in world %s!";
    public static final String ERROR_GROUP_UNKNOWN_META_KEY_GLOBAL = RED + "%s isn't a set metadata key for group %s!";
    public static final String ERROR_GROUP_UNKNOWN_META_KEY_WORLD = RED + "%s isn't a set metadata key for group %s in world %s!";
    public static final String ERROR_UNKNOWN = RED + "An unknown error has occurred!";
    public static final String ERROR_PROMOTE_PLAYER_MULTIPLE_GROUPS = RED + "Player %s is in multiple groups! Please use /groupadd or /groupset.";
    public static final String ERROR_PROMOTE_CHOICE_NOT_FOUND = RED + "There wasn't an avaliable group to promote the player %s to by the name of %s.";
    public static final String ERROR_PROMOTE_SUBGROUPS = AQUA + "There were multiple groups that player could be promoted to:";
    public static final String ERROR_PROMOTE_SUBGROUPS_CHOICE = AQUA + "There were no groups named %s that the player could be promoted to.";
    public static final String ERROR_PROMOTE_SUBGROUP_VALUE = AQUA + "%s";
    public static final String ERROR_GROUP_ALREADY_EXISTS = RED + "A group by the name of %s already exists!";

    /* Console error messages */
    public static final String ERROR_SQL_NOT_CONNECTED = "Failed to connect to the MySQL database. Please ensure it is turned on and the credentials are correct.";

    /* Success messages */
    public static final String SUCCESS_GROUP_CREATE = GREEN + "Successfully created group \"%s\" with priority %s.";
    public static final String SUCCESS_GROUP_DELETE = GREEN + "Successfully deleted group \"%s.\"";
    public static final String SUCCESS_GROUP_META_SET_GLOBAL = GREEN + "Successfully set metadata key %s to value %s for group %s.";
    public static final String SUCCESS_GROUP_META_SET_WORLD = GREEN + "Successfully set metadata key %s to value %s for group %s in world %s.";
    public static final String SUCCESS_GROUP_META_CLEAR_GLOBAL = GREEN + "Successfully unset metadata at key %s for group %s.";
    public static final String SUCCESS_GROUP_META_CLEAR_WORLD = GREEN + "Successfully unset metadata at key %s for group %s in world %s.";
    public static final String SUCCESS_GROUP_ADD_PARENT = GREEN + "Successfully added parent %s to group %s.";
    public static final String SUCCESS_GROUP_REMOVE_PARENT = GREEN + "Successfully removed parent %s from group %s.";
    public static final String SUCCESS_GROUP_ADD_GLOBAL = GREEN + "Successfully added permission %s to group %s globally.";
    public static final String SUCCESS_GROUP_ADD_WORLD = GREEN + "Successfully added permission %s to group %s in world %s.";
    public static final String SUCCESS_GROUP_REMOVE_GLOBAL = GREEN + "Successfully removed permission %s from group %s.";
    public static final String SUCCESS_GROUP_REMOVE_WORLD = GREEN + "Successfully removed permission %s from group %s in world %s.";
    public static final String SUCCESS_GROUP_ADD_TEMP_WORLD = GREEN + "Successfully added permission %s to group %s for %s seconds globally.";
    public static final String SUCCESS_GROUP_ADD_TEMP_GLOBAL = GREEN + "Successfully added permission %s to group %s for %s seconds in world %s.";
    public static final String SUCCESS_GROUP_REMOVE_TEMP_GLOBAL = GREEN + "Successfully removed temporary permission %s from group %s.";
    public static final String SUCCESS_GROUP_REMOVE_TEMP_WORLD = GREEN + "Successfully removed temporary permission %s from group %s in world %s.";
    public static final String SUCCESS_PLAYER_ADD_GLOBAL = GREEN + "Successfully added permission %s to player %s.";
    public static final String SUCCESS_PLAYER_ADD_WORLD = GREEN + "Successfully added permission %s to player %s in world %s.";
    public static final String SUCCESS_PLAYER_REMOVE_GLOBAL = GREEN + "Successfully removed permission %s from player %s.";
    public static final String SUCCESS_PLAYER_REMOVE_WORLD = GREEN + "Successfully removed permission %s from player %s in world %s.";
    public static final String SUCCESS_PLAYER_ADDTEMP_GLOBAL = GREEN + "Successfully added permission %s to player %s for %s seconds.";
    public static final String SUCCESS_PLAYER_ADDTEMP_WORLD = GREEN + "Successfully added permission %s to player %s in world %s for %s seconds.";
    public static final String SUCCESS_PLAYER_REMOVETEMP_GLOBAL = GREEN + "Successfully removed temporary permission %s from player %s.";
    public static final String SUCCESS_PLAYER_REMOVETEMP_WORLD = GREEN + "Successfully removed temporary permission %s from player %s in world %s.";
    public static final String SUCCESS_PLAYER_PROMOTE = GREEN + "Successfully promoted player %s from group %s to group %s.";
    public static final String SUCCESS_PLAYER_META_SET_GLOBAL = GREEN + "Successfully set metadata key %s to value %s for player %s.";
    public static final String SUCCESS_PLAYER_META_SET_WORLD = GREEN + "Successfully set metadata key %s to value %s for player %s in world %s.";
    public static final String SUCCESS_PLAYER_META_CLEAR_GLOBAL = GREEN + "Successfully unset metadata at key %s for player %s.";
    public static final String SUCCESS_PLAYER_META_CLEAR_WORLD = GREEN + "Successfully unset metadata at key %s for player %s in world %s.";
    public static final String SUCCESS_PLAYER_SET_GROUP = GREEN + "Successfully set player %s's group to %s.";
    public static final String SUCCESS_PLAYER_ADD_GROUP = GREEN + "Successfully added group %s to player %s.";

    /* Info messages */
    public static final String GROUPS_WITH_NODE = AQUA + "Values set by groups with that node:";
    public static final String GROUP_NODE_VALUE = AQUA + "%s";
    public static final String PLAYER_NODE_VALUE = AQUA + "%s";
    public static final String PLAYER_TEMP_NODES = AQUA + "Values added by temporary player permissions:";
    public static final String PLAYER_TEMP_NODE_VALUE = AQUA + "%s";
    public static final String PLAYER_TRANSIENT_NODES = AQUA + "Values added by transient player permissions:";
    public static final String PLAYER_TRANSIENT_NODE_VALUE = AQUA + "%s";
    public static final String VALUE_OF_PERMISSION = AQUA + "The value of permission " + GOLD + "%s" + AQUA + " is: " + GOLD + "%s" + AQUA + ".";
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
