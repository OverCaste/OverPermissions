package com.overmc.overpermissions.internal.localentities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.overmc.overpermissions.internal.datasources.PermissionEntityDataSource;

/**
 * a {@link LocalPermissionEntity} with transient node support.
 */
public abstract class LocalTransientPermissionEntity extends LocalPermissionEntity {
    // Transient data.
    private final Set<String> transientNodes = new HashSet<>();

    // Concurrency locks
    private final ReadWriteLock transientNodesLock = new ReentrantReadWriteLock();

    public LocalTransientPermissionEntity(PermissionEntityDataSource dataSource, boolean wildcardSupport) {
        super(dataSource, wildcardSupport);
    }

    @Override
    protected Set<String> getAllNodes( ) {
        return Sets.union(super.getAllNodes(), transientNodes);
    }

    @Override
    protected Iterable<ReadWriteLock> getAllNodeLocks( ) {
        return Iterables.concat(super.getAllNodeLocks(), Arrays.asList(transientNodesLock));
    }

    protected boolean addInternalTransientPermissionNode(String permissionNode) {
        transientNodesLock.writeLock().lock();
        boolean success;
        try {
            success = transientNodes.add(permissionNode.toLowerCase());
        } finally {
            transientNodesLock.writeLock().unlock();
        }
        if (success) {
            recalculatePermission(permissionNode);
        }
        return success;
    }

    protected boolean addInternalTransientPermissions(Collection<String> permissions) {
        transientNodesLock.writeLock().lock();
        boolean success = false;
        try {
            success = transientNodes.addAll(permissions);
        } finally {
            transientNodesLock.writeLock().unlock();
        }
        if (success) {
            recalculatePermissions(permissions);
        }
        return success;
    }

    protected boolean removeInternalTransientPermissionNode(String permissionNode) {
        transientNodesLock.writeLock().lock();
        boolean success;
        try {
            success = transientNodes.remove(permissionNode.toLowerCase());
        } finally {
            transientNodesLock.writeLock().unlock();
        }
        if (success) {
            recalculatePermission(permissionNode);
        }
        return success;
    }

    protected boolean hasInternalTransientPermissionNode(String permissionNode) {
        transientNodesLock.readLock().lock();
        try {
            return transientNodes.contains(permissionNode.toLowerCase());
        } finally {
            transientNodesLock.readLock().unlock();
        }
    }

    protected Collection<String> getInternalTransientNodes( ) {
        transientNodesLock.readLock().lock();
        try {
            return new ArrayList<>(transientNodes);
        } finally {
            transientNodesLock.readLock().unlock();
        }
    }
}
