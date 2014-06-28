package com.overmc.overpermissions;

public enum PermissionChangeCause {
    /** An event was called through a command via a player */
    PLAYER,
    /** An event was called through a command via the console */
    CONSOLE,
    /** An event was called by the native api. */
    API,
    /** An event was called because a temporary permission node timed out */
    TEMP_TIMEOUT
}
