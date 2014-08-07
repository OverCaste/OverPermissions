package com.overmc.overpermissions.api;

/**
 * A skeleton version of a {@link GroupManager} implementation.
 */
public abstract class AbstractGroupManager implements GroupManager {
    @Override
    public boolean doesGroupExist(String name) {
        return getGroup(name) != null;
    }
}
