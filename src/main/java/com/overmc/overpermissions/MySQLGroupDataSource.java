package com.overmc.overpermissions;

import static com.overmc.overpermissions.MySQLManager.attemptClose;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.collect.ImmutableCollection;
import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;

public class MySQLGroupDataSource implements GroupDataSource {
    private final MySQLManager sqlManager;

    private final String groupName;
    private final Future<Integer> uid;

    public MySQLGroupDataSource(ExecutorService executor, MySQLManager sqlManager, String groupName) {
        this.sqlManager = sqlManager;
        this.groupName = groupName;
        uid = executor.submit(new Callable<Integer>() {
            @Override
            public Integer call( ) throws Exception {
                return getDatabaseUid();
            }
        });
    }

    private int getDatabaseUid( ) {
        CallableStatement cst = null;
        try {
            Connection con = sqlManager.getConnection();
            cst = con.prepareCall("SELECT uid FROM Permission_Groups WHERE name = ?");
            cst.setString(1, groupName);
            ResultSet rs = cst.executeQuery();
            if (rs.first() && rs.isLast()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) { // TODO properly handle sql exceptions
            e.printStackTrace();
        } finally {
            attemptClose(cst);
        }
        return -1;
    }

    int getUid( ) {
        try {
            return uid.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new AssertionError("Execution error where none was expected.", e);
        }
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
    public Collection<String> getGlobalPermissions( ) {
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
    public Collection<TemporaryPermissionEntry> getGlobalTempPermissions( ) {
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
    public Map<String, String> getGlobalMetadata( ) {
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
    public void addGlobalPermission(String permissionNode) {
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
    public void addGlobalPermissions(Iterable<String> permissionNodes) {
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
    public void removeGlobalPermission(String permissionNode) {
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
    public void removeGlobalPermissions(Iterable<String> permissionNodes) {
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
    public void addPermission(String worldName, String permissionNode) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("INSERT IGNORE INTO Group_World_Permissions(permission_uid, world_uid, group_uid) VALUES (select_or_insert_permission(?), select_or_insert_world(?), ?)");
            pst.setString(1, permissionNode);
            pst.setString(2, worldName);
            pst.setInt(3, getUid());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void addPermissions(String worldName, Iterable<String> permissionNodes) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("INSERT IGNORE INTO Group_World_Permissions(permission_uid, world_uid, group_uid) VALUES (select_or_insert_permission(?), select_or_insert_world(?), ?)");
            pst.setString(2, worldName);
            pst.setInt(3, getUid());
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
    public void removePermission(String worldName, String permissionNode) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("DELETE FROM Group_World_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid=?");
            pst.setString(1, permissionNode);
            pst.setString(2, worldName);
            pst.setInt(3, getUid());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void addTempPermission(String worldName, String permissionNode, long timeInMillis) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("INSERT IGNORE INTO Group_World_Temporary_Permissions(permission_uid, world_uid, group_uid, timeout) VALUES (select_or_insert_permission(?), select_or_insert_world(?), ?, ?)");
            pst.setString(1, permissionNode);
            pst.setString(2, worldName);
            pst.setInt(3, getUid());
            pst.setLong(4, timeInMillis);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void addTempPermissions(String worldName, ImmutableCollection<TemporaryPermissionEntry> permissionNodes) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("INSERT IGNORE INTO Group_World_Temporary_Permissions(permission_uid, world_uid, group_uid, timeout) VALUES (select_or_insert_permission(?), select_or_insert_world(?), ?, ?)");
            pst.setString(2, worldName);
            pst.setInt(3, getUid());
            for (TemporaryPermissionEntry e : permissionNodes) {
                pst.setString(1, e.getNode());
                pst.setLong(4, e.getTimeInMillis());
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
    public void removeTempPermission(String worldName, String permissionNode) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("DELETE FROM Group_World_Temporary_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid=?");
            pst.setString(1, permissionNode);
            pst.setString(2, worldName);
            pst.setInt(3, getUid());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void addGlobalTempPermission(long timeInMillis, String permissionNode) {
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
    public void addGlobalTempPermissions(Collection<TemporaryPermissionEntry> entries) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("INSERT IGNORE INTO Group_Global_Temporary_Permissions(permission_uid, group_uid, timeout) VALUES (select_or_insert_permission(?), ?, ?)");
            pst.setInt(2, getUid());
            for (TemporaryPermissionEntry e : entries) {
                pst.setString(1, e.getNode());
                pst.setLong(3, e.getTimeInMillis());
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
    public void removeGlobalTempPermission(String permissionNode) {
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
    public void removeGlobalTempPermissions(Collection<TemporaryPermissionEntry> entries) {
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
    public void setGlobalMeta(String key, String value) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("INSERT INTO Group_Global_Meta(group_uid, meta_key, meta_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value = ?");
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
    public void removeGlobalMeta(String key) {
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
    public void setGlobalMetaEntries(Collection<MetadataEntry> entries) {
        PreparedStatement insertStatement = null;
        PreparedStatement deleteStatement = null;
        try {
            Connection con = sqlManager.getConnection();
            insertStatement = con.prepareStatement("INSERT INTO Group_Global_Meta(group_uid, meta_key, meta_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value = ?");
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
    public void setMeta(String worldName, String key, String value) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("INSERT INTO Group_World_Meta(group_uid, world_uid, meta_key, meta_value) VALUES (?, (SELECT uid FROM Worlds WHERE name=?), ?, ?) ON DUPLICATE KEY UPDATE value = ?");
            pst.setInt(1, getUid());
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
    public void removeMeta(String worldName, String key) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Group_World_Meta WHERE group_uid=? AND world_uid=(SELECT uid FROM Worlds WHERE name=?) AND meta_key=?");
            pst.setInt(1, getUid());
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
    public void setMetaEntries(String worldName, Collection<MetadataEntry> entries) {
        PreparedStatement insertStatement = null;
        PreparedStatement deleteStatement = null;
        try {
            Connection con = sqlManager.getConnection();
            insertStatement = con
                    .prepareStatement("INSERT INTO Group_World_Meta(group_uid, world_uid, meta_key, meta_value) VALUES (?, (SELECT uid FROM Worlds WHERE name=?), ?, ?) ON DUPLICATE KEY UPDATE value = ?");
            deleteStatement = con.prepareStatement("DELETE FROM Group_World_Meta WHERE group_uid=? AND world_uid=(SELECT uid FROM Worlds WHERE name=?) AND meta_key=?");
            insertStatement.setInt(1, getUid());
            deleteStatement.setInt(1, getUid());
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
    public Collection<String> getPermissions(String worldName) {
        ArrayList<String> ret = new ArrayList<>();
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("SELECT permission_node FROM Group_World_Permissions INNER JOIN Permissions ON Group_World_Permissions.permission_uid = Permissions.uid WHERE world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid=?");
            pst.setString(1, worldName);
            pst.setInt(2, getUid());
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
    public Collection<TemporaryPermissionEntry> getTempPermissions(String worldName) {
        ArrayList<TemporaryPermissionEntry> ret = new ArrayList<>();
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("SELECT permission_node, timeout FROM Group_World_Temporary_Permissions INNER JOIN Permissions ON Group_World_Temporary_Permissions.permission_uid = Permissions.uid WHERE world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid=?");
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
    public Map<String, String> getMetadata(String worldName) {
        HashMap<String, String> ret = new HashMap<String, String>(64);
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT meta_key, meta_value FROM Group_World_Meta WHERE world_uid=(SELECT uid FROM Worlds WHERE name=?) AND group_uid = ?)");
            pst.setString(1, worldName);
            pst.setInt(2, getUid());
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
}
