package com.overmc.overpermissions.internal.databases.mysql;

import com.google.common.base.Preconditions;
import com.overmc.overpermissions.internal.datasources.GroupManagerDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import static com.overmc.overpermissions.internal.databases.mysql.MySQLManager.attemptClose;

public class MySQLGroupManagerDataSource implements GroupManagerDataSource {
    private final MySQLManager sqlManager;
    private final String defaultGroup;

    public MySQLGroupManagerDataSource(MySQLManager sqlManager, String defaultGroup) {
        this.sqlManager = sqlManager;
        this.defaultGroup = defaultGroup;
    }

    @Override
    public Collection<GroupDataEntry> getGroupEntries( ) {
        ArrayList<GroupDataEntry> groups = new ArrayList<GroupDataEntry>();
        PreparedStatement pst = null;
        try(Connection con = sqlManager.getConnection()) {
            pst = con.prepareStatement("SELECT name, priority FROM Permission_Groups");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                groups.add(new GroupDataEntry(rs.getString("name"), rs.getInt("priority")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
        return groups;
    }

    @Override
    public void createGroup(String name, int priority) {
        PreparedStatement pst = null;
        try(Connection con = sqlManager.getConnection()) {
            pst = con.prepareStatement("INSERT INTO Permission_Groups(priority, name) VALUES (?, ?)");
            pst.setInt(1, priority);
            pst.setString(2, name);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }

    @Override
    public void deleteGroup(String name) {
        Preconditions.checkArgument(!defaultGroup.equals(name), "You can't delete the default group!");
        PreparedStatement pst = null;
        try(Connection con = sqlManager.getConnection()) {
            //Set player groups to the default once they get kicked.
            pst = con.prepareStatement("UPDATE Player_Groups SET group_uid=(SELECT uid from Permission_Groups WHERE name=?) WHERE group_uid=(SELECT uid from Permission_Groups WHERE name = ?)");
            pst.setString(1, defaultGroup);
            pst.setString(2, name);
            pst.executeUpdate();
            pst = con.prepareStatement("DELETE FROM Permission_Groups WHERE name = ?");
            pst.setString(1, name);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            attemptClose(pst);
        }
    }
}
