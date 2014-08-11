package com.overmc.overpermissions.internal.databases;

import java.sql.Connection;

import com.overmc.overpermissions.exceptions.DatabaseConnectionException;

public interface ConnectionPool {
    /**Retrieve a regular connection to the database*/
    public Connection getConnection( ) throws DatabaseConnectionException;
    /**Shutdown this pool data source, clearing all required resources.*/
    public void shutdown( );
}
