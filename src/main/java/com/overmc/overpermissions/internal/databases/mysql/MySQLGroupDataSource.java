package com.overmc.overpermissions.internal.databases.mysql;

import static com.overmc.overpermissions.internal.databases.mysql.MySQLManager.attemptClose;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

import com.overmc.overpermissions.api.*;
import com.overmc.overpermissions.internal.datasources.GroupDataSource;
import com.overmc.overpermissions.internal.datasources.PermissionEntityDataSource;

public class MySQLGroupDataSource implements GroupDataSource {
    private final MySQLManager sqlManager;

    private final String groupName;
    private volatile int groupUid = -1;

    public MySQLGroupDataSource(ExecutorService executor, MySQLManager sqlManager, String groupName) {
        this.sqlManager = sqlManager;
        this.groupName = groupName;
    }

    int getUid( ) {
        if (groupUid == -1) { // Double checked locking lazy initialization... Now ain't that a mouthful.
            synchronized (this) {
                if (groupUid == -1) {
                    groupUid = readDatabaseGroupUid();
                }
            }
        }
        return groupUid;
    }

    private int readDatabaseGroupUid( ) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT uid FROM Permission_Groups WHERE name = ?");
            pst.setString(1, groupName);
            ResultSet rs = pst.executeQuery();
            if (rs.first() && rs.isLast()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) { // TODO properly handle sql exceptions
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return -1;
    }

    @Override
    public int getPriority( ) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT priority FROM Permission_Groups WHERE uid=?");
            pst.setInt(1, getUid());
            ResultSet rs = pst.executeQuery();
            if (rs.first() && rs.isLast()) {
                return rs.getInt("priority");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return -1;
    }

    @Override
    public Collection<String> getPermissions( ) {
        ArrayList<String> ret = new ArrayList<>();
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            // pst = con.prepareStatement("SELECT permission_node FROM Permissions WHERE uid=(SELECT permission_uid FROM Group_Global_Permissions WHERE group_uid=?)"); //This one works, but is dumber.
            pst = con.prepareStatement("SELECT permission_node from Group_Global_Permissions INNER JOIN Permissions ON Group_Global_Permissions.permission_uid=Permissions.uid WHERE group_uid=?");
            pst.setInt(1, getUid());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String permissionNode = rs.getString("permission_node");
                ret.add(permissionNode);
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
                    .prepareStatement("SELECT permission_node, timeout FROM Group_Global_Temporary_Permissions INNER JOIN Permissions ON Group_Global_Temporary_Permissions.permission_uid=Permissions.uid WHERE group_uid=?");
            pst.setInt(1, getUid());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String permissionNode = rs.getString("permission_node");
                long timeout = rs.getLong("timeout");
                ret.add(new TemporaryPermissionEntry(permissionNode, timeout));
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
        Map<String, String> ret = new HashMap<>();
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT meta_key, meta_value FROM Group_Global_Meta WHERE group_uid=?");
            pst.setInt(1, getUid());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String key = rs.getString("meta_key");
                String value = rs.getString("meta_value");
                ret.put(key, value);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return ret;
    }

    @Override
    public void addPermission(String permissionNode) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("INSERT IGNORE INTO Group_Global_Permissions(permission_uid, group_uid) VALUES (select_or_insert_permission(?), ?)");
            pst.setString(1, permissionNode);
            pst.setInt(2, getUid());
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
            pst = con.prepareStatement("INSERT IGNORE INTO Group_Global_Permissions(permission_uid, group_uid) VALUES (select_or_insert_permission(?), ?)");
            pst.setInt(2, getUid());
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
            pst = con.prepareStatement("DELETE FROM Group_Global_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND group_uid=?");
            pst.setString(1, permissionNode);
            pst.setInt(2, getUid());
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
            pst = con.prepareStatement("DELETE FROM Group_Global_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND group_uid=?");
            pst.setInt(2, getUid());
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
            pst = con.prepareStatement("INSERT IGNORE INTO Group_Global_Temporary_Permissions(permission_uid, group_uid, timeout) VALUES (select_or_insert_permission(?), ?, ?)");
            pst.setString(1, permissionNode);
            pst.setInt(2, getUid());
            pst.setLong(3, timeInMillis);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void addTempPermissions(Iterable<TemporaryPermissionEntry> entries) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("INSERT IGNORE INTO Group_Global_Temporary_Permissions(permission_uid, group_uid, timeout) VALUES (select_or_insert_permission(?), ?, ?)");
            pst.setInt(2, getUid());
            for (TemporaryPermissionEntry e : entries) {
                pst.setString(1, e.getNode());
                pst.setLong(3, e.getExpirationTime());
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
            pst = con.prepareStatement("DELETE FROM Group_Global_Temporary_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND group_uid=?");
            pst.setString(1, permissionNode);
            pst.setInt(2, getUid());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void removeTempPermissions(Iterable<TemporaryPermissionEntry> entries) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Group_Global_Temporary_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND group_uid=?");
            pst.setInt(2, getUid());
            for (TemporaryPermissionEntry e : entries) {
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
            pst = con.prepareStatement("INSERT INTO Group_Global_Meta(group_uid, meta_key, meta_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE meta_value = ?");
            pst.setInt(1, getUid());
            pst.setString(2, key);
            pst.setString(3, value);
            pst.setString(4, value);
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
            pst = con.prepareStatement("DELETE FROM Group_Global_Meta WHERE group_uid=? AND meta_key=?");
            pst.setInt(1, getUid());
            pst.setString(2, key);
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
            insertStatement = con.prepareStatement("INSERT INTO Group_Global_Meta(group_uid, meta_key, meta_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE meta_value = ?");
            deleteStatement = con.prepareStatement("DELETE FROM Group_Global_Meta WHERE group_uid=? AND meta_key=?");
            insertStatement.setInt(1, getUid());
            deleteStatement.setInt(1, getUid());
            for (MetadataEntry e : entries) {
                if (e.getValue() == null) {
                    deleteStatement.setString(2, e.getKey());
                    deleteStatement.addBatch();
                } else {
                    insertStatement.setString(2, e.getKey());
                    insertStatement.setString(3, e.getValue());
                    insertStatement.setString(4, e.getValue());
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
    public void addParent(PermissionGroup parent) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("INSERT IGNORE INTO Group_Parents(group_uid, parent_uid) VALUES (?, (SELECT uid FROM Permission_Groups WHERE name=?))");
            pst.setInt(1, getUid());
            pst.setString(2, parent.getName());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void removeParent(PermissionGroup parent) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Group_Parents WHERE group_uid=? AND parent_uid=(SELECT uid FROM Permission_Groups WHERE name=?)");
            pst.setInt(1, getUid());
            pst.setString(2, parent.getName());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public Collection<String> getParents( ) {
        ArrayList<String> ret = new ArrayList<>();
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT name FROM Group_Parents INNER JOIN Permission_Groups ON Group_Parents.parent_uid = Permission_Groups.uid WHERE group_uid=?");
            pst.setInt(1, getUid());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ret.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return ret;
    }

    @Override
    public Collection<String> getChildren( ) {
        ArrayList<String> ret = new ArrayList<>();
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT name FROM Group_Parents INNER JOIN Permission_Groups ON Group_Parents.group_uid = Permission_Groups.uid WHERE parent_uid=?");
            pst.setInt(1, getUid());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                ret.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return ret;
    }

    @Override
    public PermissionEntityDataSource createWorldDataSource(String name) {
        return new MySQLGroupWorldDataSource(sqlManager, this, name);
    }
}
