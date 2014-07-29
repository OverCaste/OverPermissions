package com.overmc.overpermissions.internal.databases.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import com.overmc.overpermissions.api.TemporaryNodeBatch;
import com.overmc.overpermissions.internal.datasources.TemporaryPermissionEntityDataSource;

public class MySQLTempGroupDataSource implements TemporaryPermissionEntityDataSource {
    private final MySQLManager sqlManager;
    private final String groupName;

    public MySQLTempGroupDataSource(MySQLManager sqlManager, String groupName) {
        this.sqlManager = sqlManager;
        this.groupName = groupName;
    }

    @Override
    public TemporaryNodeBatch getTempPermissions( ) {
        TemporaryNodeBatch.Builder builder = TemporaryNodeBatch.builder();
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement(""
                    + "SELECT permission_node, timeout "
                    + "FROM Group_Global_Temporary_Permissions "
                    + "INNER JOIN Permissions ON Group_Global_Temporary_Permissions.permission_uid=Permissions.uid "
                    + "WHERE group_uid=(SELECT uid FROM Permission_Groups WHERE name=?)");
            pst.setString(1, groupName);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                builder.addGlobalNode(rs.getString("permission_node"), rs.getLong("timeout") - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            }
            pst.close();
            pst = con.prepareStatement(""
                    + "SELECT permission_node, timeout, name "
                    + "FROM Group_World_Temporary_Permissions "
                    + "INNER JOIN Permissions ON Group_World_Temporary_Permissions.permission_uid = Permissions.uid "
                    + "INNER JOIN Worlds ON Group_World_Temporary_Permissions.world_uid = Worlds.uid "
                    + "WHERE group_uid=(SELECT uid FROM Permission_Groups WHERE name=?)");
            pst.setString(1, groupName);
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
