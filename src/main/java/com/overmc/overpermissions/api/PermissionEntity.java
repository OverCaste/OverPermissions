package com.overmc.overpermissions.api;

import java.util.Map;

import com.google.common.collect.ImmutableList;

/**
 * An entity capable of having permission nodes set on it.
 */
public interface PermissionEntity extends UniqueEntity {
    /**
     * Checks whether this entity has a specific global permission set. Its actual value could be true or false. To check the permission key's actual value use {@link #getGlobalPermission(String)}
     * 
     * @param permission the permission to be checked for existence.
     * @return whether this entity has that specific permission set.
     * 
     * @see #getGlobalPermission(String)
     * @see #addPermissionNode(String)
     * @see #hasPermission(String, String)
     */
    public boolean hasGlobalPermission(String permission);

    /**
     * Checks whether this entity has a specific permission set in a specific world. Its actual value could be true or false. To check the permission key's actual value use {@link #getPermission(String, int)}
     * 
     * @param permission the permission to be checked for existence.
     * @param worldName the name of world for the permission to be checked in.
     * @return whether this entity has that specific permission set.
     */
    public boolean hasPermission(String permission, String worldName);

    /**
     * Checks whether this entity has a specific permission node in its global store.<br>
     * To check the value of an actual permission, use {@link #hasGlobalPermission(String)} and {@link #getGlobalPermission(String)}
     * 
     * @param permissionNode the node to check for existence.
     * @return whether this entity has that specific permission node in its store.
     */
    public boolean hasGlobalPermissionNode(String permissionNode);

    /**
     * Checks whether this entity has a specific permission node set for a specific world in its global store.<br>
     * To check the value of an actual permission, use {@link #hasPermission(String, String)} and {@link #getPermission(String, String)}
     * 
     * @param permissionNode the node to be checked for existence.
     * @param worldName the name of the world in which the node resides.
     * @return whether this entity has that specific permission node in the specified world in its store.
     */
    public boolean hasPermissionNode(String permissionNode, String worldName);

    /**
     * @param permission the permission to retrieve the value of.
     * @return the value of the specific permission, or false if it isn't set.
     * 
     * @see #hasGlobalPermission(String)
     * @see #addPermissionNode(String)
     */
    public boolean getGlobalPermission(String permission);

    /**
     * Retrieve a permission from both the global, and local store of 'permanent' permissions, and any subtypes.
     * 
     * @param permission the permission to retrieve the value of.
     * @param worldName the name of the world for the permission to be retrieved in.
     * @return the value of the permission in the specified world, or false if it isn't set.
     * 
     * @see TemporaryPermissionEntity
     * @see TransientPermissionEntity
     */
    public boolean getPermission(String permission, String worldName);

    /**
     * @param permissionNode the node to be added. Has to be an alphanumeric string, with periods ".", but can have the prefixes "-" and "+" for fine tuning.
     * @return whether or not the specified node was successfully added to this group.
     * 
     * @see #addBatchPermissions(ImmutableList)
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
     * @return true if any nodes were successfully set, false otherwise.
     * 
     * @see #addPermissionNode(String)
     */
    public boolean addBatchPermissions(NodeBatch nodes);

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
     * @return true if any nodes were successfully removed, false otherwise.
     */
    public boolean removeBatchPermissions(NodeBatch nodes);

    /**
     * Retrieve a new node batch of all the nodes represented directly by this entity.<br>
     * Parents' or subtypes' nodes or can't be retrieved with this method.
     * 
     * @return the batch of nodes represented by this entity.
     * 
     * @see TemporaryPermissionEntity#getTempPermissionNodes()
     * @see TransientPermissionEntity#getTransientPermissionNodes()
     */
    public NodeBatch getPermissionNodes( );

    /**
     * Retrieves a new map of all of the permission-value pairs represented by this entity.<br>
     * This method will not poll global nodes, or parents' or subtypes' nodes.
     * 
     * @return the map of key-value pairs that represents this entity.
     * 
     * @see #getPermissionNodes()
     */
    public Map<String, Boolean> getPermissionValues(String world);

    /**
     * Retrieves a new map of all of the permission-value pairs represented by this entity.<br>
     * This method will not poll world nodes, or parents' or subtypes' nodes.
     * 
     * @return the map of key-value pairs that represents this entity.
     * 
     * @see #getPermissionNodes()
     */
    public Map<String, Boolean> getGlobalPermissionValues( );
}
