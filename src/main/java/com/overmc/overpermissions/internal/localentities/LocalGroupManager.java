package com.overmc.overpermissions.internal.localentities;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.base.Preconditions;
import com.overmc.overpermissions.api.GroupManager;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.exceptions.GroupAlreadyExistsException;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.*;
import com.overmc.overpermissions.internal.datasources.GroupManagerDataSource.GroupDataEntry;

public class LocalGroupManager implements GroupManager {
    private final GroupManagerDataSourceFactory sourceFactory;
    private final GroupManagerDataSource dataSource;
    private final TemporaryPermissionManager tempManager;

    private final Map<String, LocalGroup> groups = new HashMap<>();
    private final ReadWriteLock groupLock = new ReentrantReadWriteLock(); // Need fine grained control for atomic operations.

    private final boolean wildcardSupport;

    public LocalGroupManager(GroupManagerDataSourceFactory sourceFactory, TemporaryPermissionManager tempManager, boolean wildcardSupport) {
        this.sourceFactory = sourceFactory;
        this.tempManager = tempManager;
        this.dataSource = sourceFactory.createGroupManagerDataSource();

        this.wildcardSupport = wildcardSupport;
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
            LocalGroup group = new LocalGroup(sourceFactory.createGroupDataSource(lowerName), tempManager, name, priority, wildcardSupport);
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
        LocalGroup deletedGroup;
        groupLock.writeLock().lock();
        try {
            success = ((deletedGroup = groups.remove(name.toLowerCase())) != null);
        } finally {
            groupLock.writeLock().unlock();
        }
        if (success) {
            dataSource.deleteGroup(name);
            tempManager.cancelTemporaryPermissions(deletedGroup); // Cancel the deleted group's temporary permissions
        }
        return success;
    }

    @Override
    public boolean doesGroupExist(String name) {
        Preconditions.checkNotNull(name, "name");
        groupLock.readLock().lock();
        try {
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
            for (PermissionGroup g : groups.values()) {
                tempManager.cancelTemporaryPermissions(g);
            }
            groups.clear();
            Collection<GroupDataEntry> groupDataEntries = dataSource.getGroupEntries();
            for (GroupDataEntry entry : groupDataEntries) {
                String name = entry.getGroupName();
                String lowerName = name.toLowerCase();
                LocalGroup group = new LocalGroup(sourceFactory.createGroupDataSource(lowerName), tempManager, name, entry.getPriority(), wildcardSupport);
                group.reloadMetadata();
                group.reloadPermissions();
                group.reloadWorldMetadata();
                group.reloadWorldPermissions();
                groups.put(lowerName, group);
            }
        } finally {
            groupLock.writeLock().unlock();
        }
        for (LocalGroup g : groups.values()) {
            g.reloadParentsAndChildren(this);
            tempManager.initializeGroupTemporaryPermissions(g); // Wouldn't make much sense to recalculate group's parents and children when they aren't defined yet.
        }
    }
}
