package com.overmc.overpermissions.internal.databases.mysql;

import java.sql.*;
import java.util.UUID;

import com.overmc.overpermissions.internal.databases.AbstractUUIDDataSource;

public class MySQLUUIDHandler extends AbstractUUIDDataSource {
    private final MySQLManager sqlManager;

    public MySQLUUIDHandler(MySQLManager sqlManager) {
        this.sqlManager = sqlManager;
    }

    @Override
    public void setNameUuid(String name, UUID uuid) {
        PreparedStatement pst = null;
        try(Connection con = sqlManager.getConnection()) {
            pst = con
                    .prepareStatement("INSERT INTO Uuid_Player_Maps(username, player_uid) VALUES (?, (SELECT uid FROM Players WHERE lower_uid=? AND upper_uid=?)) ON DUPLICATE KEY UPDATE player_uid=(SELECT uid FROM Players WHERE lower_uid=? AND upper_uid=?)");
            pst.setString(1, name);
            pst.setLong(2, uuid.getLeastSignificantBits());
            pst.setLong(3, uuid.getMostSignificantBits());
            pst.setLong(4, uuid.getLeastSignificantBits());
            pst.setLong(5, uuid.getMostSignificantBits());
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
        try(Connection con = sqlManager.getConnection()) {
            pst = con.prepareStatement("SELECT lower_uid, upper_uid FROM Uuid_Player_Maps WHERE username=? INNER JOIN Players ON Uuid_Player_Maps.player_uid=Players.uid");
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
}
