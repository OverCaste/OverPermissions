package com.overmc.overpermissions.internal.databases.mysql;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.common.base.Preconditions;
import com.overmc.overpermissions.exceptions.DatabaseConnectionException;
import com.overmc.overpermissions.internal.databases.SingleConnectionPool;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLHikariConnectionPool extends SingleConnectionPool {
    private final HikariDataSource connectionPool;

    private MySQLHikariConnectionPool(String serverName, String serverPort, String dbName, String dbUsername, String dbPassword, String pluginName) throws SQLException {
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
        if(pluginName != null) {
            config.setPoolName("for " + pluginName); //Code from hikari: LOGGER.info("HikariCP pool {} is shutting down.", configuration.getPoolName());
        }
        config.setConnectionTimeout(1000L); //Timeout after one second instead of the 30 default.
        connectionPool = new HikariDataSource(config);
    }
    
    public static class Builder {
        // === REQUIRED PARAMETERS ===
        private final String dbName;
        
        // === OPTIONAL PARAMETERS ===
        private String dbUsername = "root";
        private String serverName = "localhost";
        private String serverPort = "3306";
        private String dbPassword = "";
        private String pluginName = null;
        
        public Builder(String dbName) {
            Preconditions.checkNotNull(dbName, "database name");
            this.dbName = dbName;
        }
        
        public Builder setServerName(String serverName) {
            Preconditions.checkNotNull(serverName, "server name");
            this.serverName = serverName;
            return this;
        }
        
        public Builder setServerPort(String serverPort) {
            Preconditions.checkNotNull(serverPort, "server port");
            this.serverPort = serverPort;
            return this;
        }
        
        public Builder setDatabaseUsername(String dbUsername) {
            this.dbUsername = dbUsername;
            return this;
        }
        
        public Builder setDatabasePassword(String dbPassword) {
            this.dbPassword = dbPassword;
            return this;
        }
        
        /**
         * @param pluginName The name of the owner of this pool: 'HikariCP pool for [pluginName] is shutting down.'
         */
        public Builder setPluginName(String pluginName) {
            this.pluginName = pluginName;
            return this;
        }
        
        public MySQLHikariConnectionPool build( ) throws SQLException {
            return new MySQLHikariConnectionPool(serverName, serverPort, dbName, dbUsername, dbPassword, pluginName);
        }
    }
    
    @Override
    public Connection getConnection( ) throws DatabaseConnectionException {
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
