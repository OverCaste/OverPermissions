package com.overmc.overpermissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.base.Preconditions;
import com.overmc.overpermissions.GroupManagerDataSource.GroupDataEntry;
import com.overmc.overpermissions.api.GroupManager;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.exceptions.GroupAlreadyExistsException;

public class LocalGroupManager implements GroupManager {
    private final DataSourceFactory sourceFactory;
    private final GroupManagerDataSource dataSource;
    private final TemporaryPermissionManager tempManager;

    private final Map<String, LocalGroup> groups = new HashMap<>();
    private final ReadWriteLock groupLock = new ReentrantReadWriteLock(); // Need fine grained control for atomic operations.

    public LocalGroupManager(DataSourceFactory sourceFactory, TemporaryPermissionManager tempManager) {
        this.sourceFactory = sourceFactory;
        this.tempManager = tempManager;
        this.dataSource = sourceFactory.createGroupManagerDataSource();
    }

    @Override
    public void createGroup(String name, int priority) throws GroupAlreadyExistsException {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkArgument(priority >= 0, "priority has to be greater or equal to 0.");
        String lowerName = name.toLowerCase();
        groupLock.writeLock().lock();
        try {
            if (groups.containsKey(lowerName)) {
                throw new GroupAlreadyExistsException("A group by the name of " + lowerName + " already exists.");
            }
            LocalGroup group = new LocalGroup(sourceFactory.createGroupDataSource(lowerName), tempManager, name, priority);
            group.reloadMetadata();
            group.reloadParentsAndChildren(this);
            group.reloadPermissions();
            group.reloadWorldMetadata();
            group.reloadWorldPermissions();
            groups.put(lowerName, group);
        } finally {
            groupLock.writeLock().unlock();
        }
        dataSource.createGroup(name, priority);
    }

    @Override
    public boolean deleteGroup(String name) {
        Preconditions.checkNotNull(name, "name");
        boolean success;
        groupLock.writeLock().lock();
        try {
            success = (groups.remove(name.toLowerCase()) != null);
        } finally {
            groupLock.writeLock().unlock();
        }
        if (success) {
            dataSource.deleteGroup(name);
        }
        return success;
    }

    @Override
    public boolean doesGroupExist(String name) {
        Preconditions.checkNotNull(name, "name");
        groupLock.readLock().lock();
        try {
            System.out.println("Groups: " + groups.toString());
            return groups.containsKey(name.toLowerCase());
        } finally {
            groupLock.readLock().unlock();
        }
    }

    @Override
    public PermissionGroup getGroup(String name) {
        Preconditions.checkNotNull(name, "name");
        groupLock.readLock().lock();
        try {
            return groups.get(name.toLowerCase());
        } finally {
            groupLock.readLock().unlock();
        }
    }

    @Override
    public Iterable<PermissionGroup> getGroups( ) {
        ArrayList<PermissionGroup> ret = new ArrayList<PermissionGroup>(groups.size());
        groupLock.readLock().lock();
        try {
            ret.addAll(groups.values());
            return ret;
        } finally {
            groupLock.readLock().unlock();
        }
    }

    public void reloadGroups( ) {
        groupLock.writeLock().lock();
        try {
            groups.clear();
            Collection<GroupDataEntry> groupNames = dataSource.getGroupNames();
            for (GroupDataEntry entry : groupNames) {
                System.out.println("Loading group: " + entry.groupName);
                String name = entry.groupName;
                String lowerName = name.toLowerCase();
                LocalGroup group = new LocalGroup(sourceFactory.createGroupDataSource(lowerName), tempManager, name, entry.priority);
                group.reloadMetadata();
                group.reloadParentsAndChildren(this);
                group.reloadPermissions();
                group.reloadWorldMetadata();
                group.reloadWorldPermissions();
                groups.put(lowerName, group);
            }
        } finally {
            groupLock.writeLock().unlock();
        }
    }
}
