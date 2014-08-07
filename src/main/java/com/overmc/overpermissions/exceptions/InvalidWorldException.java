package com.overmc.overpermissions.exceptions;

public class InvalidWorldException extends RuntimeException {
    private static final long serialVersionUID = -7655560112478980438L;

    public InvalidWorldException( ) {
        super();
    }

    public InvalidWorldException(String message) {
        super(message);
    }

    public InvalidWorldException(Throwable cause) {
        super(cause);
    }

    public InvalidWorldException(String message, Throwable cause) {
        super(message, cause);
    }
}
