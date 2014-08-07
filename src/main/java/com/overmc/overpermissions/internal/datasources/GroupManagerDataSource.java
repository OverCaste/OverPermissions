package com.overmc.overpermissions.internal.datasources;

import java.util.Collection;

public interface GroupManagerDataSource {

    Collection<GroupDataEntry> getGroupEntries( );

    void createGroup(String name, int priority);

    void deleteGroup(String name);

    public static class GroupDataEntry {
        private final String groupName;
        private final int priority;

        public GroupDataEntry(String groupName, int priority) {
            this.groupName = groupName;
            this.priority = priority;
        }

        public String getGroupName( ) {
            return groupName;
        }

        public int getPriority( ) {
            return priority;
        }
    }
}
