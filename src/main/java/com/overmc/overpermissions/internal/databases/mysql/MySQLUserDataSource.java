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
import java.util.UUID;

import com.overmc.overpermissions.api.MetadataEntry;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.TemporaryPermissionEntry;
import com.overmc.overpermissions.internal.datasources.PermissionEntityDataSource;
import com.overmc.overpermissions.internal.datasources.UserDataSource;

public class MySQLUserDataSource implements UserDataSource {
    private final MySQLManager sqlManager;

    private final UUID uuid;
    private volatile int userUid = -1;
    private volatile boolean playerCheckedForExistance = false;

    public MySQLUserDataSource(MySQLManager sqlManager, UUID uuid) {
        this.sqlManager = sqlManager;
        this.uuid = uuid;
    }

    int getOrCreateUid( ) {
        if (userUid == -1) { // Double checked locking lazy initialization
            synchronized (this) {
                if (userUid == -1) {
                    userUid = getOrCreateDatabaseUserUid();
                }
            }
        }
        return userUid;
    }

    int getUid( ) {
        if (playerCheckedForExistance) {
            return userUid;
        }
        if (userUid == -1) {
            synchronized (this) {
                if (userUid == -1) {
                    userUid = getDatabaseUserUid();
                }
                playerCheckedForExistance = true;
            }
        }
        return userUid;
    }

    private int getOrCreateDatabaseUserUid( ) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT select_or_insert_player(?, ?)");
            pst.setLong(1, uuid.getLeastSignificantBits());
            pst.setLong(2, uuid.getMostSignificantBits());
            ResultSet rs = pst.executeQuery();
            if (rs.first()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) { // TODO properly handle sql exceptions
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return -1;
    }

    private int getDatabaseUserUid( ) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT uid FROM Players WHERE lower_uid = ? AND upper_uid = ?");
            pst.setLong(1, uuid.getLeastSignificantBits());
            pst.setLong(2, uuid.getMostSignificantBits());
            ResultSet rs = pst.executeQuery();
            if (rs.first()) {
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
    public Collection<String> getPermissions( ) {
        ArrayList<String> ret = new ArrayList<>();
        int uid = getUid();
        if (uid == -1) { // Don't create new SQL entries for gets on players that don't exist
            return ret;
        }
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT permission_node from Player_Global_Permissions INNER JOIN Permissions ON Player_Global_Permissions.permission_uid=Permissions.uid WHERE player_uid=?");
            pst.setInt(1, uid);
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
        int uid = getUid();
        if (uid == -1) {
            return ret;
        }
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con
                    .prepareStatement("SELECT permission_node, timeout FROM Player_Global_Temporary_Permissions INNER JOIN Permissions ON Player_Global_Temporary_Permissions.permission_uid=Permissions.uid WHERE player_uid=?");
            pst.setInt(1, uid);
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
        int uid = getUid();
        if (uid == -1) {
            return ret;
        }
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT meta_key, meta_value FROM Player_Global_Meta WHERE player_uid=?");
            pst.setInt(1, uid);
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
            pst = con.prepareStatement("INSERT IGNORE INTO Player_Global_Permissions(permission_uid, player_uid) VALUES (select_or_insert_permission(?), ?)");
            pst.setString(1, permissionNode);
            pst.setInt(2, getOrCreateUid());
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
            pst = con.prepareStatement("INSERT IGNORE INTO Player_Global_Permissions(permission_uid, player_uid) VALUES (select_or_insert_permission(?), ?)");
            pst.setInt(2, getOrCreateUid());
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
        int uid = getUid();
        if (uid == -1) {
            return;
        }
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Player_Global_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND player_uid=?");
            pst.setString(1, permissionNode);
            pst.setInt(2, uid);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void removePermissions(Iterable<String> permissionNodes) {
        int uid = getUid();
        if (uid == -1) {
            return;
        }
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Player_Global_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND player_uid=?");
            pst.setInt(2, getOrCreateUid());
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
            pst = con.prepareStatement("INSERT IGNORE INTO Player_Global_Temporary_Permissions(permission_uid, player_uid, timeout) VALUES (select_or_insert_permission(?), ?, ?)");
            pst.setString(1, permissionNode);
            pst.setInt(2, getOrCreateUid());
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
            pst = con.prepareStatement("INSERT IGNORE INTO Player_Global_Temporary_Permissions(permission_uid, player_uid, timeout) VALUES (select_or_insert_permission(?), ?, ?)");
            pst.setInt(2, getOrCreateUid());
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
        int uid = getUid();
        if (userUid == -1) {
            return;
        }
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Player_Global_Temporary_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND player_uid=?");
            pst.setString(1, permissionNode);
            pst.setInt(2, uid);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void removeTempPermissions(Iterable<TemporaryPermissionEntry> entries) {
        int uid = getUid();
        if (userUid == -1) {
            return;
        }
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Player_Global_Temporary_Permissions WHERE permission_uid=(SELECT uid FROM Permissions WHERE permission_node=?) AND player_uid=?");
            pst.setInt(2, uid);
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
            pst = con.prepareStatement("INSERT INTO Player_Global_Meta(player_uid, meta_key, meta_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE meta_value = ?");
            pst.setInt(1, getOrCreateUid());
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
        int uid = getUid();
        if (userUid == -1) {
            return;
        }
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Player_Global_Meta WHERE player_uid=? AND meta_key=?");
            pst.setInt(1, uid);
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
            insertStatement = con.prepareStatement("INSERT INTO Player_Global_Meta(player_uid, meta_key, meta_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE meta_value = ?");
            deleteStatement = con.prepareStatement("DELETE FROM Player_Global_Meta WHERE player_uid=? AND meta_key=?");
            insertStatement.setInt(1, getOrCreateUid());
            deleteStatement.setInt(1, getOrCreateUid());
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
            pst = con.prepareStatement("INSERT IGNORE INTO Player_Groups(group_uid, player_uid) VALUES ((SELECT uid FROM Permission_Groups WHERE name=?), ?)");
            pst.setString(1, parent.getName());
            pst.setInt(2, getOrCreateUid());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void removeParent(PermissionGroup parent) {
        int uid = getUid();
        if (userUid == -1) {
            return;
        }
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Player_Groups WHERE group_uid=(SELECT uid FROM Permission_Groups WHERE name=?) AND player_uid=?");
            pst.setString(1, parent.getName());
            pst.setInt(2, uid);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void setParent(PermissionGroup parent) {
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("DELETE FROM Player_Groups WHERE player_uid=?");
            pst.setInt(1, getOrCreateUid());
            pst.executeUpdate();
            pst.close();
            pst = con.prepareStatement("INSERT INTO Player_Groups(group_uid, player_uid) VALUES ((SELECT uid FROM Permission_Groups WHERE name=?), ?)");
            pst.setString(1, parent.getName());
            pst.setInt(2, getOrCreateUid());
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
        int uid = getUid();
        if (userUid == -1) {
            return ret;
        }
        PreparedStatement pst = null;
        try {
            Connection con = sqlManager.getConnection();
            pst = con.prepareStatement("SELECT name FROM Player_Groups INNER JOIN Permission_Groups ON Player_Groups.group_uid = Permission_Groups.uid WHERE player_uid=?");
            pst.setInt(1, uid);
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
    public PermissionEntityDataSource createWorldDataSource(String worldName) {
        return new MySQLUserWorldDataSource(sqlManager, this, worldName);
    }

    @Override
    public boolean doesUserExist( ) {
        return getUid() != -1;
    }
}
