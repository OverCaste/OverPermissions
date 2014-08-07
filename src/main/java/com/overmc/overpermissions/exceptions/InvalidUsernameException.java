package com.overmc.overpermissions.exceptions;

public class InvalidUsernameException extends RuntimeException {
    private static final long serialVersionUID = -7655560112478980438L;

    public InvalidUsernameException( ) {
        super();
    }

    public InvalidUsernameException(String message) {
        super(message);
    }

    public InvalidUsernameException(Throwable cause) {
        super(cause);
    }

    public InvalidUsernameException(String message, Throwable cause) {
        super(message, cause);
    }
}
