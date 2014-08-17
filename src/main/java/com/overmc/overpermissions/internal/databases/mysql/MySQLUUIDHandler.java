package com.overmc.overpermissions.internal.databases.mysql;

import java.sql.*;
import java.util.UUID;

import com.overmc.overpermissions.internal.databases.AbstractUUIDDataSource;

public class MySQLUUIDHandler extends AbstractUUIDDataSource {
    private final MySQLManager sqlManager;

    public MySQLUUIDHandler(MySQLManager sqlManager, boolean forceOnlineMode) {
        super(forceOnlineMode);
        this.sqlManager = sqlManager;
    }

    @Override
    public void setNameUuid(String name, UUID uuid) {
        PreparedStatement pst = null;
        try (Connection con = sqlManager.getConnection()) {
            pst = con.prepareStatement("INSERT IGNORE INTO Players(lower_uid, upper_uid) VALUES (?, ?)"); //Ensure a player uuid exists for this player.
            pst.setLong(1, uuid.getLeastSignificantBits());
            pst.setLong(2, uuid.getMostSignificantBits());
            pst.executeUpdate();
            pst.close();
            pst = con
                    .prepareStatement("INSERT INTO Uuid_Player_Maps(username, player_uid, last_seen) VALUES ("
                            + "?, "
                            + "(SELECT uid FROM Players WHERE lower_uid=? AND upper_uid=?), "
                            + "?"
                            + ") ON DUPLICATE KEY UPDATE player_uid="
                            + "(SELECT uid FROM Players WHERE lower_uid=? AND upper_uid=?), "
                            + "last_seen=?");
            pst.setString(1, name);
            pst.setLong(2, uuid.getLeastSignificantBits());
            pst.setLong(3, uuid.getMostSignificantBits());
            pst.setLong(4, System.currentTimeMillis());
            pst.setLong(5, uuid.getLeastSignificantBits());
            pst.setLong(6, uuid.getMostSignificantBits());
            pst.setLong(7, System.currentTimeMillis());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            MySQLManager.attemptClose(pst);
        }
    }

    @Override
    public UUID getDatabaseNameUuid(String name) {
        PreparedStatement pst = null;
        try (Connection con = sqlManager.getConnection()) {
            pst = con.prepareStatement("SELECT lower_uid, upper_uid FROM Uuid_Player_Maps INNER JOIN Players ON Uuid_Player_Maps.player_uid=Players.uid WHERE username=?");
            pst.setString(1, name);
            ResultSet rs = pst.executeQuery();
            if (rs.first()) {
                return new UUID(rs.getLong("upper_uid"), rs.getLong("lower_uid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            MySQLManager.attemptClose(pst);
        }
        return null;
    }

    @Override
    public String getLastSeenName(UUID uuid) {
        PreparedStatement pst = null;
        try (Connection con = sqlManager.getConnection()) {
            pst = con.prepareStatement("SELECT username FROM Uuid_Player_Maps WHERE lower_uid=? AND upper_uid=? ORDER BY last_seen DESC LIMIT 1");
            pst.setLong(1, uuid.getLeastSignificantBits());
            pst.setLong(2, uuid.getMostSignificantBits());
            ResultSet rs = pst.executeQuery();
            if (rs.first()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            MySQLManager.attemptClose(pst);
        }
        return null;
    }
}
