package com.overmc.overpermissions.api;

import com.google.common.collect.ImmutableList;

public interface TransientPermissionEntity {
    /**
     * Adds a transient permission node to this entity. A transient node doesn't persist in a database of any kind. <br>
     * Transient nodes are generally faster to change.
     * 
     * @param permissionNode the transient node to be added.
     * @param worldName the name of the world in which the permission should be added.
     * @return whether a transient node of the specified name was added in the specified world.
     */
    public boolean addTransientPermissionNode(String permissionNode, String worldName);
    
    /**
     * Adds a transient permission node to this entity. A transient node doesn't persist in a database of any kind. <br>
     * Transient nodes are generally faster to change.
     * 
     * @param permissionNode the transient node to be added.
     * @return whether a transient node of the specified name was added.
     */
    public boolean addGlobalTransientPermissionNode(String permissionNode);

    /**
     * Removes a transient permission node from this entity. A transient node doesn't persist in a database of any kind. <br>
     * Transient nodes are generally faster to change.
     * 
     * @param permissionNode the transient node to be removed.
     * @param worldName the name of the world in which the permission should be removed.
     * @return whether a transient node of the specified name was removed.
     */
    public boolean removeTransientPermissionNode(String permissionNode, String worldName);
    
    /**
     * Removes a transient permission node from this entity. A transient node doesn't persist in a database of any kind. <br>
     * Transient nodes are generally faster to change.
     * 
     * @param permissionNode the transient node to be removed.
     * @return whether the specified transient node existed and was removed.
     */
    public boolean removeGlobalTransientPermissionNode(String permissionNode);

    /**
     * @param permissionNode the transient node to be checked for existence.
     * @param worldName the name of the world in which to check for the specified permission node.
     * @return whether a transient node of the specified name existed in this entity.
     */
    public boolean hasTransientPermissionNode(String permissionNode, String worldName);
    
    /**
     * @param permissionNode the transient node to be checked for existence.
     * @return whether a transient node of the specified name existed in this entity.
     */
    public boolean hasGlobalTransientPermissionNode(String permissionNode);

    /**
     * @return an {@link ImmutableList} of every transient permission node that exists in this entity.
     */
    public NodeBatch getTransientPermissionNodes( );
}
