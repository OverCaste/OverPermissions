package com.overmc.overpermissions.api;

import com.google.common.collect.ImmutableList;

/**
 * An entity capable of having permission nodes set on it.
 */
public interface PermissionEntity {
    /**
     * Checks whether this entity has a specific global permission set. It's actual value could be true or false. To check the permission key's actual value use {@link #getPermission(String)}
     * 
     * @param permission the permission to be checked for.
     * @return whether this entity has that specific permission set.
     * 
     * @see #getPermission(String)
     * @see #addPermissionNode(String)
     * @see #hasPermission(String, String)
     */
    public boolean hasGlobalPermission(String permission);

    /**
     * Checks whether this entity has a specific permission set in a specific world. It's actual value could be true or false. To check the permission key's actual value use {@link #getPermission(String, int)}
     * 
     * @param permission the permission to be checked for.
     * @param worldName the name of world for the permission to be checked in.
     * @return whether this entity has that specific permission set.
     */
    public boolean hasPermission(String permission, String worldName);

    /**
     * @param permission
     * @return the value of the specific permission, or false if it isn't set.
     * 
     * @see #hasGlobalPermission(String)
     * @see #addPermissionNode(String)
     */
    public boolean getPermission(String permission);

    /**
     * @param permissionNode the node to be added. Has to be an alphanumeric string, with periods ".", but can have the prefixes "-" and "+" for fine tuning.
     * @return whether or not the specified node was successfully added to this group.
     * 
     * @see #addBatchPermissionNodes(ImmutableList)
     */
    public boolean addGlobalPermissionNode(String permissionNode);

    /**
     * @param permissionNode the node to be added. Has to be an alphanumeric string, with periods ".", but can have the prefixes "-" and "+" for fine tuning.
     * @param worldName the name of the world for the permission to be set in.
     * @return whether or not the specific node was successfully added to this group in the specified world.
     * 
     * @see #addGlobalPermissionNode(String)
     */
    public boolean addPermissionNode(String permissionNode, String worldName);

    /**
     * Add an entire batch of permission nodes at once. This is recommended for performance reasons.
     * 
     * @param nodes the batch of nodes to be changed.
     * @return true if all nodes were successfully set, false otherwise.
     * 
     * @see #addPermissionNode(String)
     */
    public boolean addBatchPermissionNodes(NodeBatch nodes);

    /**
     * @param permissionNode the node to be removed. Has to be an alphanumeric string, with periods ".", but can have the prefixes "-" and "+" for fine tuning.
     * @return whether the node existed, and was successfully removed.
     */
    public boolean removeGlobalPermissionNode(String permissionNode);

    /**
     * @param permissionNode the node to be removed. Has to be an alphanumeric string, with periods ".", but can have the prefixes "-" and "+" for fine tuning.
     * @param worldName the name of the world for the node to be removed in.
     * @return whether the node existed, and was successfully removed.
     */
    public boolean removePermissionNode(String permissionNode, String worldName);

    /**
     * @param nodes the batch of nodes to be removed.
     * @return true if all nodes were successfully removed, false otherwise.
     */
    public boolean removeBatchPermissionNodes(NodeBatch nodes);

    /**
     * @return the batch of nodes represented by this entity.
     */
    public NodeBatch getAllPermissionNodes( );
}
