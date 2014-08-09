package com.overmc.overpermissions.exceptions;

public class DatabaseConnectionException extends RuntimeException {
    private static final long serialVersionUID = 3415494631439640130L;

    public DatabaseConnectionException() {
        super("OverPermissions can't connect to the database.");
    }
}
