package com.overmc.overpermissions.internal.databases.mysql;

import java.sql.Connection;
import java.sql.SQLException;

import com.overmc.overpermissions.internal.databases.PoolDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLHikariCPPoolDataSource implements PoolDataSource {
    private final HikariDataSource connectionPool;

    public MySQLHikariCPPoolDataSource(String serverName, String serverPort, String dbName, String dbUsername, String dbPassword) throws SQLException {
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
        connectionPool = new HikariDataSource(config);
    }

    @Override
    public Connection getBaseConnection( ) throws SQLException {
        return getDatabaseConnection(); // No difference
    }

    @Override
    public Connection getDatabaseConnection( ) throws SQLException {
        return connectionPool.getConnection();
    }

    @Override
    public void shutdown( ) {
        connectionPool.shutdown();
    }
}
