package com.overmc.overpermissions.api;

import com.overmc.overpermissions.exceptions.GroupAlreadyExistsException;

public interface GroupManager {
    /**
     * This method will throw a {@link GroupAlreadyExistsException} if the group with the specified name already exists. Use {@link #doesGroupExist(String)} to check beforehand instead of catching this.
     *
     * @param name the name of the group to be created.
     * @param priority the priority of the group to be created. Groups with higher priority will override the metadata of those with lower priority.
     */
    public void createGroup(String name, int priority);

    /**
     * @param name the name of the group to be deleted.
     * @return whether the group was successfully deleted.
     */
    public boolean deleteGroup(String name);

    /**
     * @param name The name of the group to be checked.
     * @return whether or not the group exists.
     */
    public boolean doesGroupExist(String name);

    /**
     * @param name The name of the group to be retrieved.
     * @return the group with the specified name, or null if it doesn't exist.
     * 
     * @see #doesGroupExist(String)
     */
    public PermissionGroup getGroup(String name);

    /**
     * @return A list of all currently resolvable groups.
     */
    public Iterable<PermissionGroup> getGroups( );
}
