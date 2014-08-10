package com.overmc.overpermissions.internal.databases.mysql;

import java.sql.Connection;
import java.sql.SQLException;

import com.overmc.overpermissions.exceptions.DatabaseConnectionException;
import com.overmc.overpermissions.internal.databases.SingleConnectionPool;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLHikariConnectionPool extends SingleConnectionPool {
    private final HikariDataSource connectionPool;

    public MySQLHikariConnectionPool(String serverName, String serverPort, String dbName, String dbUsername, String dbPassword) throws SQLException {
        super(dbUsername, dbPassword, "jdbc:mysql://" + serverName + ":" + serverPort + "/", dbName);
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        config.addDataSourceProperty("serverName", serverName);
        config.addDataSourceProperty("port", serverPort);
        config.addDataSourceProperty("databaseName", dbName);
        config.addDataSourceProperty("user", dbUsername);
        config.addDataSourceProperty("password", dbPassword);
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "1024");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true"); // We don't have that many big batches.
        config.setPoolName("for OverPermissions JDBC"); //Code from hikari: LOGGER.info("HikariCP pool {} is shutting down.", configuration.getPoolName());
        config.setConnectionTimeout(1000L); //Timeout after one second instead of the 30 default.
        connectionPool = new HikariDataSource(config);
    }

    @Override
    public Connection getDatabaseConnection( ) throws DatabaseConnectionException {
        try {
            return connectionPool.getConnection();
        } catch (SQLException ex) {
            throw MySQLManager.handleSqlException(ex);
        }
    }

    @Override
    public void shutdown( ) {
        connectionPool.shutdown();
    }
}
