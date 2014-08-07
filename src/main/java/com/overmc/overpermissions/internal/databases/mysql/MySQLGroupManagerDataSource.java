package com.overmc.overpermissions.internal.databases.mysql;

import static com.overmc.overpermissions.internal.databases.mysql.MySQLManager.attemptClose;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

import com.overmc.overpermissions.internal.datasources.GroupManagerDataSource;

public class MySQLGroupManagerDataSource implements GroupManagerDataSource {
    private final MySQLManager sqlManager;

    public MySQLGroupManagerDataSource(MySQLManager sqlManager) {
        this.sqlManager = sqlManager;
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
        PreparedStatement pst = null;
        try(Connection con = sqlManager.getConnection()) {
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
