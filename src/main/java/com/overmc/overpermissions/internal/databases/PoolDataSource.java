package com.overmc.overpermissions.internal.databases;

import java.sql.Connection;
import java.sql.SQLException;

public interface PoolDataSource {
    /**Retrieve a connection without a database being specified.
     * @throws SQLException */
    public Connection getBaseConnection( ) throws SQLException;
    /**Retrieve a regular connection to the database*/
    public Connection getDatabaseConnection( ) throws SQLException;
    /**Shutdown this pool data source, clearing all required resources.*/
    public void shutdown( );
}
