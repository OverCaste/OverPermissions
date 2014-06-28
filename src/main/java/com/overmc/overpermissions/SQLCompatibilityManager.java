package com.overmc.overpermissions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.overmc.overpermissions.uuid.UUIDFetcher;

public class SQLCompatibilityManager {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private int configurationSchemaVersion;
    private final OverPermissions plugin;

    public SQLCompatibilityManager(OverPermissions plugin) {
        this.plugin = plugin;
        configurationSchemaVersion = plugin.getConfig().getInt("internals.sql-schema-version", 0);
        if (configurationSchemaVersion > CURRENT_SCHEMA_VERSION) {
            plugin.getLogger().warning("You can't downgrade OverPermissions versions without wiping your database and resetting the internals.sql-schema-version field.");
            plugin.getLogger().warning("If you simply mistakenly changed the value in your configuration, change it back to what it's value originally was.");
        }
    }

    public void init( ) {
        CompatibilityUpdateStep[] steps = {new CompatibilityUpdateStepOne()};

        boolean success = true;

        int currentStep = configurationSchemaVersion;

        try {
            while (currentStep < CURRENT_SCHEMA_VERSION) {
                steps[currentStep].update(plugin.getSQLManager());
                currentStep++;
            }
        } catch (Exception e) {
            success = false;
            plugin.getLogger().info("Failure updating sql schema from version " + (currentStep) + " to version " + (currentStep + 1) + ": " + e.getMessage());
            e.printStackTrace();
        }

        if (success) {
            configurationSchemaVersion = CURRENT_SCHEMA_VERSION;
            plugin.getConfig().set("internals.sql-schema-version", configurationSchemaVersion);
            plugin.saveConfig();
        }
    }

    private interface CompatibilityUpdateStep {
        public void update(SQLManager manager) throws Exception;
    }

    private class CompatibilityUpdateStepOne implements CompatibilityUpdateStep {
        private void executeStatement(Connection con, String statement) throws Exception {
            Statement st = con.createStatement();
            try {
                st.execute(statement);
            } finally {
                MySQLManager.attemptClose(st);
            }
        }

        @Override
        public void update(SQLManager manager) throws Exception {
            // TODO make more versatile
            if (manager instanceof MySQLManager) {
                MySQLManager mySqlManager = (MySQLManager) manager;
                Connection con = mySqlManager.getConnection();
                // Duplicate old tables
                con.setAutoCommit(false);
                try {
                    plugin.getLogger().info("Migration: Renaming player table...");
                    executeStatement(con, "RENAME TABLE Player TO PlayerOld");
                    plugin.getLogger().info("Creating new Player table structure...");
                    executeStatement(con, "CREATE TABLE Player" // TODO eventually refactor the entire SQLManager class...
                            + "("
                            + "uid int AUTO_INCREMENT PRIMARY KEY,"
                            + "last_seen_username varchar(16),"
                            + "lower_uid BIGINT NOT NULL,"
                            + "upper_uid BIGINT NOT NULL,"
                            + "INDEX username (lower_uid, upper_uid)"
                            + ")");
                    plugin.getLogger().info("Migration: Migrating old table to new syntax...");
                    migrateUuids(con);
                    con.commit(); // Successfully committed.
                } finally {
                    con.setAutoCommit(true);
                }
            }
        }

        @SuppressWarnings("deprecation")
        private void migrateUuids(Connection con) throws Exception {
            HashMap<String, Integer> usernameUidMap = new HashMap<String, Integer>(256);
            ResultSet usernameResults = con.createStatement().executeQuery("SELECT uid, username FROM PlayerOld");
            try {
                while (usernameResults.next()) {
                    int uid = usernameResults.getInt("uid");
                    String username = usernameResults.getString("username");
                    usernameUidMap.put(username, uid);
                }
            } finally {
                MySQLManager.attemptClose(usernameResults);
            }
            Map<String, UUID> uuidMap; // Convert usernames to UUIDs
            if(Bukkit.getOnlineMode()) {
                UUIDFetcher fetcher = new UUIDFetcher(new ArrayList<String>(usernameUidMap.keySet()));
                uuidMap = fetcher.call();
            } else {
                uuidMap = new HashMap<String, UUID>(usernameUidMap.size());
                for(Map.Entry<String, Integer> e : usernameUidMap.entrySet()) {
                    uuidMap.put(e.getKey(), Bukkit.getOfflinePlayer(e.getKey()).getUniqueId()); //Offline conversion is considerably less elegant.
                }
            }
            for (Map.Entry<String, Integer> playerData : usernameUidMap.entrySet()) { // Iterate over usernames.
                if (uuidMap.containsKey(playerData.getKey().toLowerCase())) { // We've got a match!
                    UUID uuid = uuidMap.get(playerData.getKey().toLowerCase());
                    PreparedStatement pst = null;
                    try {
                        pst = con.prepareStatement("INSERT INTO Player(uid, last_seen_username, lower_uid, upper_uid) VALUES (?, ?, ?, ?)");
                        pst.setInt(1, playerData.getValue());
                        pst.setString(2, playerData.getKey());
                        pst.setLong(3, uuid.getLeastSignificantBits());
                        pst.setLong(4, uuid.getMostSignificantBits());
                        pst.executeUpdate();
                        executeStatement(con, "DELETE FROM PlayerOld WHERE uid=\"" + playerData.getValue() + "\""); // I know... sloppy, but no sqlI here.
                    } finally {
                        MySQLManager.attemptClose(pst);
                    }
                } else {
                    plugin.getLogger().warning("Failed to transfer username \"" + playerData.getKey() + ".\" UUID doesn't exist!");
                }
            }
            Statement tableEmptyStatement = null;
            ResultSet tableEmpty = null;
            try {
                tableEmptyStatement = con.createStatement();
                tableEmpty = tableEmptyStatement.executeQuery("SELECT COUNT(*) FROM PlayerOld");
                if (tableEmpty.next() && (tableEmpty.getInt(1) == 0)) {
                    plugin.getLogger().info("Migration: Deleting empty old player table...");
                    executeStatement(con, "DROP TABLE PlayerOld");
                } else {
                    plugin.getLogger().warning("Migration: There were some usernames that failed to transfer. Old player table data not deleted.");
                }
            } finally {
                MySQLManager.attemptClose(tableEmpty);
                MySQLManager.attemptClose(tableEmptyStatement);
            }
        }
    }
}
