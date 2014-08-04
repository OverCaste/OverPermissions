package com.overmc.overpermissions.internal.databases.mysql;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.overmc.overpermissions.internal.Messages;
import com.overmc.overpermissions.internal.StartException;
import com.overmc.overpermissions.internal.databases.DatabaseMultiSourceFactory;
import com.overmc.overpermissions.internal.datasources.*;

public final class MySQLManager implements DatabaseMultiSourceFactory {
    private volatile boolean databaseInitialized;

    private Connection con = null;

    private final ExecutorService executor;

    private final String dbUrl;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection( ) throws SQLException {
        if ((con == null) || con.isClosed()) {
            con = DriverManager.getConnection(dbUrl + dbName, dbUsername, dbPassword);
        }
        return con;
    }

    public MySQLManager(ExecutorService executor, String dbUrl, String dbName, String dbUsername, String dbPassword) throws Throwable {
        this.executor = executor;
        this.dbUrl = dbUrl;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        initDatabase();
    }

    private void initDatabase( ) throws StartException {
        if (databaseInitialized) {
            return;
        }
        Statement st = null;
        try {
            con = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            st = con.createStatement();
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            st.close();
            st = con.createStatement();
            st.executeUpdate("USE " + dbName);
            st.close();
            st = con.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Players"
                    + "("
                    + "uid int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "lower_uid BIGINT NOT NULL,"
                    + "upper_uid BIGINT NOT NULL,"
                    + "INDEX uuid (lower_uid, upper_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Worlds"
                    + "("
                    + "uid int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "name varchar(16) NOT NULL,"
                    + "UNIQUE KEY name (name)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Servers"
                    + "("
                    + "uid int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "name varchar(64) NOT NULL,"
                    + "UNIQUE KEY name (name)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Permissions"
                    + "("
                    + "uid int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "permission_node varchar(64) NOT NULL,"
                    + "INDEX permission_node (permission_node ASC)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Uuid_Player_Maps"
                    + "("
                    + "player_uid int UNSIGNED NOT NULL,"
                    + "username varchar(16) NOT NULL PRIMARY KEY,"
                    + "INDEX username (username ASC),"
                    + "FOREIGN KEY(player_uid) REFERENCES Players(uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Player_Permissions"
                    + "("
                    + "world_uid int UNSIGNED NOT NULL,"
                    + "permission_uid int UNSIGNED NOT NULL,"
                    + "player_uid int UNSIGNED NOT NULL,"
                    + "FOREIGN KEY(world_uid) REFERENCES Worlds(uid),"
                    + "FOREIGN KEY(permission_uid) REFERENCES Permissions(uid),"
                    + "FOREIGN KEY(player_uid) REFERENCES Players(uid),"
                    + "PRIMARY KEY(world_uid, permission_uid, player_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Player_World_Temporary_Permissions"
                    + "("
                    + "world_uid int UNSIGNED NOT NULL,"
                    + "permission_uid int UNSIGNED NOT NULL,"
                    + "player_uid int UNSIGNED NOT NULL,"
                    + "timeout bigint UNSIGNED NOT NULL,"
                    + "FOREIGN KEY(world_uid) REFERENCES Worlds(uid),"
                    + "FOREIGN KEY(permission_uid) REFERENCES Permissions(uid),"
                    + "FOREIGN KEY(player_uid) REFERENCES Players(uid),"
                    + "PRIMARY KEY(world_uid, permission_uid, player_uid)"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS Player_Global_Permissions"
                    + "("
                    + "permission_uid int UNSIGNED NOT NULL,"
                    + "player_uid int UNSIGNED NOT NULL,"
                    + "FOREIGN KEY(permission_uid) REFERENCES Permissions(uid),"
                    + "FOREIGN KEY(player_uid) REFERENCES Players(uid),"
                    + "PRIMARY KEY(permission_uid, player_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Player_Global_Temporary_Permissions"
                    + "("
                    + "permission_uid int UNSIGNED NOT NULL,"
                    + "player_uid int UNSIGNED NOT NULL,"
                    + "timeout bigint UNSIGNED NOT NULL,"
                    + "FOREIGN KEY(permission_uid) REFERENCES Permissions(uid),"
                    + "FOREIGN KEY(player_uid) REFERENCES Players(uid),"
                    + "PRIMARY KEY(permission_uid, player_uid)"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS Player_World_Meta"
                    + "("
                    + "world_uid int UNSIGNED NOT NULL,"
                    + "player_uid int UNSIGNED NOT NULL,"
                    + "meta_key varchar(50) NOT NULL,"
                    + "meta_value varchar(50) NOT NULL,"
                    + "FOREIGN KEY(world_uid) REFERENCES Worlds(uid),"
                    + "FOREIGN KEY(player_uid) REFERENCES Players(uid),"
                    + "PRIMARY KEY(world_uid, player_uid, meta_key)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Player_Global_Meta"
                    + "("
                    + "player_uid int UNSIGNED NOT NULL,"
                    + "meta_key varchar(50) NOT NULL,"
                    + "meta_value varchar(50) NOT NULL,"
                    + "FOREIGN KEY(player_uid) REFERENCES Players(uid),"
                    + "PRIMARY KEY(player_uid, meta_key)"
                    + ")");

            // Groups and their data
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Permission_Groups"
                    + "("
                    + "uid int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "priority int UNSIGNED NOT NULL,"
                    + "name varchar(64) NOT NULL,"
                    + "UNIQUE KEY name (name)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Group_Parents"
                    + "("
                    + "group_uid int UNSIGNED NOT NULL,"
                    + "parent_uid int UNSIGNED NOT NULL,"
                    + "FOREIGN KEY(group_uid) REFERENCES Permission_Groups(uid),"
                    + "FOREIGN KEY(parent_uid) REFERENCES Permission_Groups(uid),"
                    + "PRIMARY KEY(group_uid, parent_uid),"
                    + "INDEX group_uid (group_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Group_World_Permissions"
                    + "("
                    + "permission_uid int UNSIGNED NOT NULL,"
                    + "world_uid int UNSIGNED NOT NULL,"
                    + "group_uid int UNSIGNED NOT NULL,"
                    + "FOREIGN KEY(permission_uid) REFERENCES Permissions(uid),"
                    + "FOREIGN KEY(world_uid) REFERENCES Worlds(uid),"
                    + "FOREIGN KEY(group_uid) REFERENCES Permission_Groups(uid),"
                    + "PRIMARY KEY(permission_uid, world_uid, group_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Group_Global_Permissions"
                    + "("
                    + "permission_uid int UNSIGNED NOT NULL,"
                    + "group_uid int UNSIGNED NOT NULL,"
                    + "FOREIGN KEY(permission_uid) REFERENCES Permissions(uid),"
                    + "FOREIGN KEY(group_uid) REFERENCES Permission_Groups(uid),"
                    + "PRIMARY KEY(permission_uid, group_uid),"
                    + "INDEX group_uid (group_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Group_World_Temporary_Permissions"
                    + "("
                    + "permission_uid int UNSIGNED NOT NULL,"
                    + "world_uid int UNSIGNED NOT NULL,"
                    + "group_uid int UNSIGNED NOT NULL,"
                    + "timeout bigint UNSIGNED NOT NULL,"
                    + "FOREIGN KEY(permission_uid) REFERENCES Permissions(uid),"
                    + "FOREIGN KEY(world_uid) REFERENCES Worlds(uid),"
                    + "FOREIGN KEY(group_uid) REFERENCES Permission_Groups(uid),"
                    + "PRIMARY KEY(permission_uid, world_uid, group_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Group_Global_Temporary_Permissions"
                    + "("
                    + "permission_uid int UNSIGNED NOT NULL,"
                    + "group_uid int UNSIGNED NOT NULL,"
                    + "timeout bigint UNSIGNED NOT NULL,"
                    + "FOREIGN KEY(permission_uid) REFERENCES Permissions(uid),"
                    + "FOREIGN KEY(group_uid) REFERENCES Permission_Groups(uid),"
                    + "PRIMARY KEY(permission_uid, group_uid),"
                    + "INDEX group_uid (group_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Group_Global_Meta"
                    + "("
                    + "group_uid int UNSIGNED NOT NULL,"
                    + "meta_key varchar(50) NOT NULL,"
                    + "meta_value varchar(50) NOT NULL,"
                    + "FOREIGN KEY(group_uid) REFERENCES Permission_Groups(uid),"
                    + "PRIMARY KEY(group_uid, meta_key),"
                    + "INDEX group_uid (group_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Group_World_Meta"
                    + "("
                    + "group_uid int UNSIGNED NOT NULL,"
                    + "world_uid int UNSIGNED NOT NULL,"
                    + "meta_key varchar(50) NOT NULL,"
                    + "meta_value varchar(50) NOT NULL,"
                    + "FOREIGN KEY(group_uid) REFERENCES Permission_Groups(uid),"
                    + "PRIMARY KEY(group_uid, world_uid, meta_key),"
                    + "INDEX group_world (group_uid, world_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Player_Groups"
                    + "("
                    + "group_uid int UNSIGNED NOT NULL,"
                    + "player_uid int UNSIGNED NOT NULL,"
                    + "FOREIGN KEY(player_uid) REFERENCES Players(uid),"
                    + "FOREIGN KEY(group_uid) REFERENCES Permission_Groups(uid),"
                    + "PRIMARY KEY(group_uid, player_uid)"
                    + ")");

            // Utility procedures and functions
            st.executeUpdate("DROP FUNCTION IF EXISTS select_or_insert_world");
            st.executeUpdate("CREATE FUNCTION select_or_insert_world (n VARCHAR(64))" +
                    "RETURNS INT UNSIGNED " +
                    "DETERMINISTIC " +
                    "MODIFIES SQL DATA " +
                    "BEGIN " +
                    "DECLARE return_value INT UNSIGNED;" +
                    "IF EXISTS (SELECT * FROM Worlds where name = n) THEN " +
                    "    SELECT uid INTO return_value FROM Worlds WHERE name = n;" +
                    "ELSE " +
                    "    INSERT INTO Worlds (name) VALUES (n);" +
                    "    SELECT LAST_INSERT_ID() INTO return_value;" +
                    "END IF;" +
                    "RETURN return_value;" +
                    "END ");
            st.executeUpdate("DROP FUNCTION IF EXISTS select_or_insert_permission");
            st.executeUpdate("CREATE FUNCTION select_or_insert_permission (p_permission_node VARCHAR(64))" +
                    "RETURNS INT UNSIGNED " +
                    "DETERMINISTIC " +
                    "MODIFIES SQL DATA " +
                    "BEGIN " +
                    "DECLARE return_value INT UNSIGNED;" +
                    "IF EXISTS (SELECT * FROM Permissions WHERE permission_node = p_permission_node) THEN " +
                    "    SELECT uid INTO return_value FROM Permissions WHERE permission_node = p_permission_node;" +
                    "ELSE " +
                    "    INSERT INTO Permissions (permission_node) VALUES (p_permission_node);" +
                    "    SELECT LAST_INSERT_ID() INTO return_value;" +
                    "END IF;" +
                    "RETURN return_value;" +
                    "END ");
            st.executeUpdate("DROP FUNCTION IF EXISTS select_or_insert_player");
            st.executeUpdate("CREATE FUNCTION select_or_insert_player (p_lower_uid bigint, p_upper_uid bigint)" +
                    "RETURNS INT UNSIGNED " +
                    "DETERMINISTIC " +
                    "MODIFIES SQL DATA " +
                    "BEGIN " +
                    "DECLARE return_value INT UNSIGNED;" +
                    "IF EXISTS (SELECT * FROM Players WHERE lower_uid = p_lower_uid AND upper_uid = p_upper_uid) THEN " +
                    "    SELECT uid INTO return_value FROM Players WHERE lower_uid = p_lower_uid AND upper_uid = p_upper_uid;" +
                    "ELSE " +
                    "    INSERT INTO Players (lower_uid, upper_uid) VALUES (p_lower_uid, p_upper_uid);" +
                    "    SELECT LAST_INSERT_ID() INTO return_value;" +
                    "END IF;" +
                    "RETURN return_value;" +
                    "END ");
        } catch (SQLException e) {
            handleStartingSqlException(e);
        } finally {
            attemptClose(st);
        }
    }

    @Override
    public GroupDataSource createGroupDataSource(String groupName) {
        return new MySQLGroupDataSource(executor, this, groupName);
    }

    @Override
    public GroupManagerDataSource createGroupManagerDataSource( ) {
        return new MySQLGroupManagerDataSource(this);
    }

    @Override
    public UserDataSource createUserDataSource(UUID uuid) {
        return new MySQLUserDataSource(this, uuid);
    }

    public static void attemptClose(Statement st) {
        try {
            if (st != null) {
                st.close();
            }
        } catch (Throwable t) {

        }
    }

    public static void attemptClose(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Throwable t) {

        }
    }

    private static void handleStartingSqlException(SQLException ex) {
        if (ex instanceof CommunicationsException) {
            throw new StartException(Messages.format(Messages.ERROR_SQL_NOT_CONNECTED));
        }
        else if (ex instanceof SQLException) {
            if (ex.getMessage().startsWith("Unable to open a test connection")) {
                throw new StartException(Messages.format(Messages.ERROR_SQL_NOT_CONNECTED));
            }
        }
        throw new RuntimeException("An unknown SQL exception has occurred.", ex);
    }

    @Override
    public UUIDDataSource createUUIDDataSource( ) {
        return new MySQLUUIDDataSource(this);
    }

    @Override
    public TemporaryPermissionEntityDataSource createTempGroupDataSource(String groupName) {
        return new MySQLTempGroupDataSource(this, groupName);
    }

    @Override
    public TemporaryPermissionEntityDataSource createTempPlayerDataSource(UUID playerUniqueId) {
        return new MySQLTempPlayerDataSource(this, playerUniqueId);
    }
}
