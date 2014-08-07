package com.overmc.overpermissions.internal.databases.mysql;

import java.sql.*;

import com.overmc.overpermissions.exceptions.StartException;
import com.overmc.overpermissions.internal.databases.PoolDataSource;

public class MySQLVanillaPoolDataSource implements PoolDataSource {
    private final String dbUrl;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;

    private volatile Connection con;

    public MySQLVanillaPoolDataSource(String serverName, String serverPort, String dbName, String dbUsername, String dbPassword) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new StartException("Missing the required library sql connector/j");
        }
        this.dbUrl = "jdbc:mysql://" + serverName + ":" + serverPort + "/";
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    @Override
    public Connection getBaseConnection( ) throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }

    @Override
    public Connection getDatabaseConnection( ) throws SQLException {
        Connection cachedCon = con;
        if (cachedCon == null || cachedCon.isClosed()) {
            synchronized (this) {
                cachedCon = con;
                if (cachedCon == null || cachedCon.isClosed()) {
                    cachedCon = con = DriverManager.getConnection(dbUrl + dbName, dbUsername, dbPassword);
                }
            }
        }
        return cachedCon;
    }

    @Override
    public void shutdown( ) {
        synchronized(this) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
