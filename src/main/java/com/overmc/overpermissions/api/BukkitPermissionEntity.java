package com.overmc.overpermissions.api;

import org.bukkit.permissions.Permission;

/**
 * An entity capable of having Bukkit SuperPermission retrieved from it.<br>
 * You can't directly set permissions through this API, it's meant mostly as a Bridge.
 */
public interface BukkitPermissionEntity extends UniqueEntity {
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
    public boolean hasGlobalPermission(Permission permission);

    /**
     * Checks whether this entity has a specific permission set in a specific world. Its actual value could be true or false. To check the permission key's actual value use {@link #getPermission(String, int)}
     * 
     * @param permission the permission to be checked for existence.
     * @param worldName the name of world for the permission to be checked in.
     * @return whether this entity has that specific permission set.
     */
    public boolean hasPermission(Permission permission, String worldName);

    /**
     * @param permission the permission to retrieve the value of.
     * @return the value of the specific permission, or false if it isn't set.
     * 
     * @see #hasGlobalPermission(String)
     * @see #addPermissionNode(String)
     */
    public boolean getGlobalPermission(Permission permission);

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
    public boolean getPermission(Permission permission, String worldName);
}
