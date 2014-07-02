package com.overmc.overpermissions.api;

import java.util.concurrent.TimeUnit;

public interface TemporaryPermissionEntity {
    /**
     * Checks whether this group has a specific global temporary permission set.
     * 
     * @see PermissionEntity#getPermission(String)
     * @see #addGlobalTempPermissionNode(String, long, TimeUnit)
     * 
     * @param permission the permission to be checked for.
     * @return true if this group has that specific permission set.
     */
    public boolean hasGlobalTempPermission(String permission);

    /**
     * Checks whether this group has a specific temporary permission set in a specific world.
     * 
     * @param permission the permission to be checked for.
     * @param worldName the world in which the permission should be checked for.
     * @return whether the specified world had the specified permission.
     */
    public boolean hasTempPermission(String permission, String worldName);

    /**
     * Adds a specified temporary permission globally for a specified time.<br>
     * If the temporary node already exists, the existing one's time is changed to the specified time.
     * 
     * @param permissionNode the node to be added. Has to be an alphanumeric string, with periods ".", but can have the prefixes "-" and "+" for fine tuning.
     * @param time how long the specified node should be added for, in 'unit'
     * @param unit the unit for the 'time' parameter.
     * @return whether or not the specified node was successfully added to this group.
     * 
     * @see #addBatchTempPermissionNodes(TemporaryNodeBatch)
     */
    public boolean addGlobalTempPermissionNode(String permissionNode, long time, TimeUnit unit);

    /**
     * Adds a specified temporary permission in the specified world for a specified time.<br>
     * If the temporary node already exists, the existing one's time is changed to the specified time.
     * 
     * @param permissionNode the node to be added. Has to be an alphanumeric string, with periods ".", but can have the prefixes "-" and "+" for fine tuning.
     * @param worldName the world in which the temporary permission should be added.
     * @param time how long the specified node should be added for, in 'unit'
     * @param time how long the specified node should be added for, in 'unit'
     * @return whether the specified node was successfully added in the specified world.
     */
    public boolean addTempPermissionNode(String permissionNode, String worldName, long time, TimeUnit unit);

    /**
     * Add an entire batch of temporary permission nodes at once. This is recommended for performance reasons.
     * 
     * @param nodes the collection of nodes to be changed.
     * @return true if all nodes were successfully set, false otherwise.
     * 
     * @see #addPermissionNode(String)
     */
    public boolean addBatchTempPermissionNodes(TemporaryNodeBatch nodes);

    /**
     * @return the entire batch of temporary nodes that represent this entity.
     */
    public TemporaryNodeBatch getAllTempPermissionNodes( );
}
