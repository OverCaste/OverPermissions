package com.overmc.overpermissions.exceptions;

/**
 * An exception that signifies that a username lookup failed.
 * This happens if there isn't a valid Mojang account associated with the given username.
 */
public class InvalidOnlineUsernameException extends Exception {
    private static final long serialVersionUID = -3818818895373988887L;

    public InvalidOnlineUsernameException( ) {
        super();
    }

    public InvalidOnlineUsernameException(String message) {
        super(message);
    }

    public InvalidOnlineUsernameException(Throwable cause) {
        super(cause);
    }

    public InvalidOnlineUsernameException(String message, Throwable cause) {
        super(message, cause);
    }
}
