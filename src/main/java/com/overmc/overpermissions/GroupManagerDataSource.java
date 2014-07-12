package com.overmc.overpermissions;

import java.util.Collection;

public interface GroupManagerDataSource {

    Collection<GroupDataEntry> getGroupNames( );

    void createGroup(String name, int priority);

    void deleteGroup(String name);

    public static class GroupDataEntry {
        final String groupName;
        final int priority;

        public GroupDataEntry(String groupName, int priority) {
            this.groupName = groupName;
            this.priority = priority;
        }
    }
}
