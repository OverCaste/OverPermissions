package com.overmc.overpermissions.api;

import org.bukkit.OfflinePlayer;

import com.overmc.overpermissions.exceptions.InvalidUsernameException;

public interface UserManager {
    /**
     * Guaranteed to return a valid PermissionUser. An actual internal user will be created if a current one doesn't exist and you call a method that changes a persistent variable.
     * 
     * @param player the {@link OfflinePlayer} object for which to get the PermissionUser for.
     * @return the PermissionUser for the specified player.
     * 
     * @throws NullPointerException if the player parameter is null.
     * @throws InvalidUsernameException if the username of the player is invalid for a permission user.
     * 
     * @see #doesUserExist(OfflinePlayer)
     * @see #canUserExist(String)
     */
    public PermissionUser getPermissionUser(OfflinePlayer player);

    /**
     * Guaranteed to return a valid PermissionUser. An actual internal user will be created if a current one doesn't exist and you call a method that changes a persistent variable.
     * 
     * @param name the name of the PermissionUser to be retrieved.
     * @return the PermissionUser for the specified player.
     * 
     * @throws NullPointerException if the name is null.
     * @throws InvalidUsernameException if the username of the player is invalid for a permission user.
     * 
     * @see #doesUserExist(OfflinePlayer)
     * @see #canUserExist(String)
     */
    public PermissionUser getPermissionUser(String name);

    /**
     * @param player the player to be checked for existence of a {@link PermissionUser} object.
     * @return whether there is a valid {@link PermissionUser} object associated with the specified player.
     */
    public boolean doesUserExist(OfflinePlayer player);

    /**
     * @param name the name of the player to be checked for existence of a {@link PermissionUser} object.
     * @return whether there is a valid {@link PermissionUser} object associated with the specified player.
     */
    public boolean doesUserExist(String name);

    /**
     * Checks if a PermissionUser can be created for the specified name.
     * 
     * @param name the name to be checked for validity.
     * @return whether the specified name can be a valid {@link PermissionUser}.
     */
    public boolean canUserExist(String name);
}
