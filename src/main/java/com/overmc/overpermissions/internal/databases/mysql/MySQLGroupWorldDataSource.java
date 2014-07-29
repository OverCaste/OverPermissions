package com.overmc.overpermissions.internal.databases.mysql;

import static com.overmc.overpermissions.internal.databases.mysql.MySQLManager.attemptClose;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;
import com.overmc.overpermissions.internal.datasources.PermissionEntityDataSource;

public class MySQLGroupWorldDataSource implements PermissionEntityDataSource {
    private final MySQLManager sqlManager;
    private final MySQLGroupDataSource groupSource;

    private final String worldName;

    public MySQLGroupWorldDataSource(MySQLManager sqlManager, MySQLGroupDataSource groupSource, String worldName) {
        this.sqlManager = sqlManager;
        this.groupSource = groupSource;
        this.worldName = worldName;
    }

    @Override
    public void addPermission(String permissionNode) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("INSERT IGNORE INTO Group_World_Permissions(permission_uid, world_uid, group_uid) VALUES (select_or_insert_permission(?), select_or_insert_world(?), ?)");
            pst.setString(1, permissionNode);
            pst.setString(2, worldName);
            pst.setInt(3, groupSource.getUid());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void addPermissions(Iterable<String> permissionNodes) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("INSERT IGNORE INTO Group_World_Permissions(permission_uid, world_uid, group_uid) VALUES (select_or_insert_permission(?), select_or_insert_world(?), ?)");
            pst.setString(2, worldName);
            pst.setInt(3, groupSource.getUid());
            for (String node : permissionNodes) {
                pst.setString(1, node);
                pst.addBatch();
            }
            pst.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void removePermission(String permissionNode) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("DELETE FROM Group_World_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid=?");
            pst.setString(1, permissionNode);
            pst.setString(2, worldName);
            pst.setInt(3, groupSource.getUid());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void removePermissions(Iterable<String> permissionNodes) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("DELETE FROM Group_World_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid=?");
            pst.setString(2, worldName);
            pst.setInt(3, groupSource.getUid());
            for (String node : permissionNodes) {
                pst.setString(1, node);
                pst.addBatch();
            }
            pst.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void addTempPermission(String permissionNode, long timeInMillis) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("INSERT IGNORE INTO Group_World_Temporary_Permissions(permission_uid, world_uid, group_uid, timeout) VALUES (select_or_insert_permission(?), select_or_insert_world(?), ?, ?)");
            pst.setString(1, permissionNode);
            pst.setString(2, worldName);
            pst.setInt(3, groupSource.getUid());
            pst.setLong(4, timeInMillis);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void addTempPermissions(Iterable<TemporaryPermissionEntry> permissionNodes) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("INSERT IGNORE INTO Group_World_Temporary_Permissions(permission_uid, world_uid, group_uid, timeout) VALUES (select_or_insert_permission(?), select_or_insert_world(?), ?, ?)");
            pst.setString(2, worldName);
            pst.setInt(3, groupSource.getUid());
            for (TemporaryPermissionEntry e : permissionNodes) {
                pst.setString(1, e.getNode());
                pst.setLong(4, e.getExpirationTime());
                pst.addBatch();
            }
            pst.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void removeTempPermission(String permissionNode) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("DELETE FROM Group_World_Temporary_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid=?");
            pst.setString(1, permissionNode);
            pst.setString(2, worldName);
            pst.setInt(3, groupSource.getUid());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void removeTempPermissions(Iterable<TemporaryPermissionEntry> permissionNodes) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("DELETE FROM Group_World_Temporary_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid=?");
            pst.setString(2, worldName);
            pst.setInt(3, groupSource.getUid());
            for (TemporaryPermissionEntry e : permissionNodes) {
                pst.setString(1, e.getNode());
                pst.addBatch();
            }
            pst.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void setMeta(String key, String value) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("INSERT INTO Group_World_Meta(group_uid, world_uid, meta_key, meta_value) VALUES (?, (SELECT uid FROM Worlds WHERE name=?), ?, ?) ON DUPLICATE KEY UPDATE meta_value = ?");
            pst.setInt(1, groupSource.getUid());
            pst.setString(2, worldName);
            pst.setString(3, key);
            pst.setString(4, value);
            pst.setString(5, value);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void removeMeta(String key) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Group_World_Meta WHERE group_uid=? AND world_uid=(SELECT uid FROM Worlds WHERE name=?) AND meta_key=?");
            pst.setInt(1, groupSource.getUid());
            pst.setString(2, worldName);
            pst.setString(3, key);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void setMetaEntries(Iterable<MetadataEntry> entries) {
        PreparedStatement insertStatement = null;
        PreparedStatement deleteStatement = null;
        try {
            Connection con = sqlManager.getConnection();
            insertStatement = con
                    .prepareStatement("INSERT INTO Group_World_Meta(group_uid, world_uid, meta_key, meta_value) VALUES (?, (SELECT uid FROM Worlds WHERE name=?), ?, ?) ON DUPLICATE KEY UPDATE meta_value = ?");
            deleteStatement = con.prepareStatement("DELETE FROM Group_World_Meta WHERE group_uid=? AND world_uid=(SELECT uid FROM Worlds WHERE name=?) AND meta_key=?");
            insertStatement.setInt(1, groupSource.getUid());
            deleteStatement.setInt(1, groupSource.getUid());
            insertStatement.setString(2, worldName);
            deleteStatement.setString(2, worldName);
            for (MetadataEntry e : entries) {
                if (e.getValue() == null) {
                    deleteStatement.setString(3, e.getKey());
                    deleteStatement.addBatch();
                } else {
                    insertStatement.setString(3, e.getKey());
                    insertStatement.setString(4, e.getValue());
                    insertStatement.setString(5, e.getValue());
                    insertStatement.addBatch();
                }
            }
            insertStatement.executeBatch();
            deleteStatement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(insertStatement);
            attemptClose(deleteStatement);
        }
    }

    @Override
    public Collection<String> getPermissions( ) {
        ArrayList<String> ret = new ArrayList<>();
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("SELECT permission_node FROM Group_World_Permissions INNER JOIN Permissions ON Group_World_Permissions.permission_uid = Permissions.uid WHERE world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid=?");
            pst.setString(1, worldName);
            pst.setInt(2, groupSource.getUid());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ret.add(rs.getString("permission_node"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return ret;
    }

    @Override
    public Collection<TemporaryPermissionEntry> getTempPermissions( ) {
        ArrayList<TemporaryPermissionEntry> ret = new ArrayList<>();
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("SELECT permission_node, timeout FROM Group_World_Temporary_Permissions INNER JOIN Permissions ON Group_World_Temporary_Permissions.permission_uid = Permissions.uid WHERE world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid=?");
            pst.setString(1, worldName);
            pst.setInt(2, groupSource.getUid());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ret.add(new TemporaryPermissionEntry(rs.getString("permission_node"), rs.getLong("timeout")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return ret;
    }

    @Override
    public Map<String, String> getMetadata( ) {
        HashMap<String, String> ret = new HashMap<String, String>(64);
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT meta_key, meta_value FROM Group_World_Meta WHERE world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid = ?)");
            pst.setString(1, worldName);
            pst.setInt(2, groupSource.getUid());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ret.put(rs.getString("meta_key"), rs.getString("meta_value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return ret;
    }
}
