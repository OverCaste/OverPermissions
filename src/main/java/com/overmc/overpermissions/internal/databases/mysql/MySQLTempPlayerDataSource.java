package com.overmc.overpermissions.internal.databases.mysql;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.overmc.overpermissions.api.TemporaryNodeBatch;
import com.overmc.overpermissions.internal.datasources.TemporaryPermissionEntityDataSource;

public class MySQLTempPlayerDataSource implements TemporaryPermissionEntityDataSource {
    private final MySQLManager sqlManager;
    private final UUID entityUniqueId;

    public MySQLTempPlayerDataSource(MySQLManager sqlManager, UUID entityUniqueId) {
        this.sqlManager = sqlManager;
        this.entityUniqueId = entityUniqueId;
    }

    @Override
    public TemporaryNodeBatch getTempPermissions( ) {
        TemporaryNodeBatch.Builder builder = TemporaryNodeBatch.builder();
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Player_Global_Temporary_Permissions WHERE timeout < ?"); //Purge global tables of outdated temp permissions
            pst.setLong(1, System.currentTimeMillis());
            pst.executeUpdate();
            pst.close();
            pst = con.prepareStatement("DELETE FROM Player_World_Temporary_Permissions WHERE timeout < ?"); //Purge world tables of outdated temp permissions
            pst.setLong(1, System.currentTimeMillis());
            pst.executeUpdate();
            pst.close();
            pst = con.prepareStatement(""
                    + "SELECT permission_node, timeout "
                    + "FROM Player_Global_Temporary_Permissions "
                    + "INNER JOIN Permissions ON Player_Global_Temporary_Permissions.permission_uid=Permissions.uid "
                    + "WHERE player_uid=(SELECT uid FROM Players WHERE lower_uid=? AND upper_uid=?)");
            pst.setLong(1, entityUniqueId.getLeastSignificantBits());
            pst.setLong(2, entityUniqueId.getMostSignificantBits());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                builder.addGlobalNode(rs.getString("permission_node"), rs.getLong("timeout") - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            }
            pst.close();
            pst = con.prepareStatement(""
                    + "SELECT permission_node, timeout, name "
                    + "FROM Player_World_Temporary_Permissions "
                    + "INNER JOIN Permissions ON Player_World_Temporary_Permissions.permission_uid = Permissions.uid "
                    + "INNER JOIN Worlds ON Player_World_Temporary_Permissions.world_uid = Worlds.uid "
                    + "WHERE player_uid=(SELECT uid FROM Players WHERE lower_uid=? AND upper_uid=?)");
            pst.setLong(1, entityUniqueId.getLeastSignificantBits());
            pst.setLong(2, entityUniqueId.getMostSignificantBits());
            rs = pst.executeQuery();
            while (rs.next()) {
                builder.addNode(rs.getString("permission_node"), rs.getString("name"), rs.getLong("timeout") - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            }
            pst.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            MySQLManager.attemptClose(pst);
        }
        return builder.build();
    }
}
