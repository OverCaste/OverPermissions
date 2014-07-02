package com.overmc.overpermissions.exceptions;

public class GroupAlreadyExistsException extends RuntimeException {
    private static final long serialVersionUID = -2332040111236600991L;

    public GroupAlreadyExistsException( ) {
        super();
    }

    public GroupAlreadyExistsException(String message) {
        super(message);
    }

    public GroupAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    public GroupAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
