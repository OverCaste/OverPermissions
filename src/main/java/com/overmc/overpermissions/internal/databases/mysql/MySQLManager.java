package com.overmc.overpermissions.internal.databases.mysql;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;
import com.overmc.overpermissions.exceptions.DatabaseConnectionException;
import com.overmc.overpermissions.exceptions.StartException;
import com.overmc.overpermissions.internal.databases.*;
import com.overmc.overpermissions.internal.datasources.*;

public final class MySQLManager implements Database {
    public static final int GLOBAL_WORLD_UID = 1;
    public static final int GLOBAL_SERVER_UID = 1;

    private volatile boolean databaseInitialized;

    private final Logger logger;
    private final ExecutorService executor;
    private final ConnectionPool connectionPool;

    private final UUIDHandler uuidHandler;

    public Connection getConnection( ) throws SQLException, DatabaseConnectionException {
        return connectionPool.getConnection();
    }

    public MySQLManager(Logger logger, ExecutorService executor, String serverName, String serverPort, String dbName, String dbUsername, String dbPassword, boolean usePool, boolean forceOnlineMode) throws Exception {
        this.executor = executor;
        if (serverPort.length() == 0) {
            serverPort = "3306"; // The default MySQL port
        }
        String url = "jdbc:mysql://" + serverName + ":" + serverPort + "/";
        initDatabase(url, dbName, dbUsername, dbPassword); // The database needs to be created so that the connection pool doesn't throw an exception, thus this constructor overhead is necessary.
        if (usePool) {
            connectionPool = new MySQLHikariConnectionPool.Builder(dbName).setServerPort(serverPort).setDatabaseUsername(dbUsername).setDatabasePassword(dbPassword).setPluginName("OverPermissions")
                    .build();
        } else {
            connectionPool = new SingleConnectionPool(dbUsername, dbPassword, url, dbName);
        }
        uuidHandler = new MySQLUUIDHandler(this, forceOnlineMode);
        this.logger = logger;
    }

    private void initDatabase(String url, String dbName, String username, String password) throws StartException, DatabaseConnectionException {
        if (databaseInitialized) {
            return;
        }
        Connection con = null;
        Statement st = null;
        try {
            con = DriverManager.getConnection(url, username, password);
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
                    + "CONSTRAINT uuid UNIQUE (lower_uid, upper_uid)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Worlds"
                    + "("
                    + "uid int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "name varchar(64) NOT NULL,"
                    + "UNIQUE KEY name (name)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Permissions"
                    + "("
                    + "uid int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "permission_node varchar(256) NOT NULL,"
                    + "INDEX permission_node (permission_node ASC)"
                    + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS Uuid_Player_Maps"
                    + "("
                    + "player_uid int UNSIGNED NOT NULL,"
                    + "username varchar(16) NOT NULL PRIMARY KEY,"
                    + "last_seen bigint UNSIGNED NOT NULL,"
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
                    + "INDEX timeout (timeout ASC),"
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
                    + "INDEX timeout (timeout ASC),"
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
                    + "INDEX timeout (timeout ASC),"
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
                    + "INDEX timeout (timeout ASC),"
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
            try {
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
            } catch (SQLException e) {
                if(e.getErrorCode() == 1304) { //A race condition could make the CREATE FUNCTION statement occur when there is already a function defined.
                    logger.fine("A race condition stopped this instance of OverPermissions from creating the function 'select_or_insert_world.'");
                } else {
                    throw e; //Propagate.
                }
            }
            try {
                st.executeUpdate("DROP FUNCTION IF EXISTS select_or_insert_permission");
                st.executeUpdate("CREATE FUNCTION select_or_insert_permission (p_permission_node VARCHAR(256))" +
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
            } catch (SQLException e) {
                if(e.getErrorCode() == 1304) { //See above
                    logger.fine("A race condition stopped this instance of OverPermissions from creating the function 'select_or_insert_permission.'");
                } else {
                    throw e; //Propagate.
                }
            }
            try {
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
                if(e.getErrorCode() == 1304) { //See above
                    logger.fine("A race condition stopped this instance of OverPermissions from creating the function 'select_or_insert_player.'");
                } else {
                    throw e; //Propagate.
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new StartException(e.getMessage());
        } finally {
            attemptClose(st);
            attemptClose(con);
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

    public int getWorldUid(String worldName) throws DatabaseConnectionException {
        PreparedStatement pst = null;
        try (Connection con = getConnection()) {
            pst = con.prepareStatement("SELECT uid FROM Worlds WHERE name = ?");
            pst.setString(1, worldName);
            ResultSet rs = pst.executeQuery();
            if (rs.first() && rs.isLast()) {
                return rs.getInt("uid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return -1;
    }

    public int getOrCreateWorldUid(String worldName) throws DatabaseConnectionException {
        PreparedStatement pst = null;
        try (Connection con = getConnection()) {
            pst = con.prepareStatement("SELECT select_or_insert_world(?)");
            pst.setString(1, worldName);
            ResultSet rs = pst.executeQuery();
            if (rs.first() && rs.isLast()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return -1;
    }

    public String getWorldName(int worldUid) throws DatabaseConnectionException {
        PreparedStatement pst = null;
        try (Connection con = getConnection()) {
            pst = con.prepareStatement("SELECT name FROM Worlds WHERE uid = ?");
            pst.setInt(1, worldUid);
            ResultSet rs = pst.executeQuery();
            if (rs.first() && rs.isLast()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return null;
    }

    public int getPlayerUid(UUID uuid) throws DatabaseConnectionException {
        PreparedStatement pst = null;
        try (Connection con = getConnection()) {
            pst = con.prepareStatement("SELECT uid FROM Players WHERE lower_uid = ? AND upper_uid = ?");
            pst.setLong(1, uuid.getLeastSignificantBits());
            pst.setLong(2, uuid.getMostSignificantBits());
            ResultSet rs = pst.executeQuery();
            if (rs.first() && rs.isLast()) {
                return rs.getInt("uid");
            }
        } catch (SQLException e) { // TODO properly handle sql exceptions
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return -1;
    }

    public int getOrCreatePlayerUid(UUID uuid) throws DatabaseConnectionException {
        PreparedStatement pst = null;
        try (Connection con = getConnection()) {
            pst = con.prepareStatement("SELECT select_or_insert_player(?, ?)");
            pst.setLong(1, uuid.getLeastSignificantBits());
            pst.setLong(2, uuid.getMostSignificantBits());
            ResultSet rs = pst.executeQuery();
            if (rs.first() && rs.isLast()) {
                return rs.getInt("uid");
            }
        } catch (SQLException e) { // TODO properly handle sql exceptions
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return -1;
    }

    public UUID getPlayerUuid(int playerUid) throws DatabaseConnectionException {
        PreparedStatement pst = null;
        try (Connection con = getConnection()) {
            pst = con.prepareStatement("SELECT lower_uid, upper_uid FROM Players WHERE uid = ?");
            pst.setInt(1, playerUid);
            ResultSet rs = pst.executeQuery();
            if (rs.first() && rs.isLast()) {
                return new UUID(rs.getLong("upper_uid"), rs.getLong("lower_uid"));
            }
        } catch (SQLException e) { // TODO properly handle sql exceptions
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return null;
    }

    public ConnectionPool getConnectionPool( ) {
        return connectionPool;
    }

    @Override
    public UUIDHandler getUUIDHandler( ) {
        return uuidHandler;
    }

    @Override
    public TemporaryPermissionEntityDataSource createTempGroupDataSource(String groupName) {
        return new MySQLTempGroupDataSource(this, groupName);
    }

    @Override
    public TemporaryPermissionEntityDataSource createTempPlayerDataSource(UUID playerUniqueId) {
        return new MySQLTempPlayerDataSource(this, playerUniqueId);
    }

    @Override
    public void shutdown( ) {
        connectionPool.shutdown();
    }

    public static void attemptClose(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (SQLException ex) {

        }
    }

    public static void attemptClose(Statement st) {
        try {
            if (st != null) {
                st.close();
            }
        } catch (SQLException ex) {

        }
    }

    public static void attemptClose(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException ex) {

        }
    }

    public static DatabaseConnectionException handleSqlException(SQLException ex) throws DatabaseConnectionException {
        if (ex instanceof CommunicationsException) {
            return new DatabaseConnectionException();
        }
        else if (ex instanceof SQLException) {
            if (ex.getMessage().startsWith("Unable to open a test connection")) {
                return new DatabaseConnectionException();
            }
        }
        throw new RuntimeException("An unknown SQL exception has occurred.", ex);
    }
}
