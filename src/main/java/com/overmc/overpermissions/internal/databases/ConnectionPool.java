package com.overmc.overpermissions.internal.databases;

import java.sql.Connection;

import com.overmc.overpermissions.exceptions.DatabaseConnectionException;

public interface ConnectionPool {
    /**Retrieve a connection to this database without a use statement set. Use this to initialize the database.*/
    public Connection getBaseConnection( ) throws DatabaseConnectionException;
    /**Retrieve a regular connection to the database*/
    public Connection getDatabaseConnection( ) throws DatabaseConnectionException;
    /**Shutdown this pool data source, clearing all required resources.*/
    public void shutdown( );
}
