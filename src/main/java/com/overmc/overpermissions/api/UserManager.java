package com.overmc.overpermissions.api;

import java.util.UUID;

import org.bukkit.OfflinePlayer;

public interface UserManager {
    /**
     * Guaranteed to return a valid PermissionUser. An actual internal user will be created if a current one doesn't exist and you call a method that changes a persistent variable.
     * 
     * @param player the {@link OfflinePlayer} object for which to get the PermissionUser for.
     * @return the PermissionUser for the specified player.
     * 
     * @throws NullPointerException if the player parameter is null.
     */
    public PermissionUser getPermissionUser(OfflinePlayer player);

    /**
     * Guaranteed to return a valid PermissionUser. An actual internal user will be created if a current one doesn't exist and you call a method that changes a persistent variable.
     * 
     * @param uuid the {@link UUID} object for which to get the PermissionUser for.
     * @return the PermissionUser for the specified player.
     * 
     * @throws NullPointerException if the UUID argument is null.
     */
    public PermissionUser getPermissionUser(UUID uuid);

    /**
     * Guaranteed to return a valid PermissionUser. An actual internal user will be created if a current one doesn't exist and you call a method that changes a persistent variable.
     * 
     * @param name the name of the PermissionUser to be retrieved.
     * @return the PermissionUser for the specified player.
     * 
     * @throws NullPointerException if the name is null.
     */
    public PermissionUser getPermissionUser(String name);
    
    /**
     * @param player the player to be checked for existence of a {@link PermissionUser} object.
     * @return whether there is a valid {@link PermissionUser} object associated with the specified player.
     */
    public boolean doesUserExist(OfflinePlayer player);
    
    /**
     * @param uuid the {@link UUID} of the player to be checked for existence of a {@link PermissionUser} object.
     * @return whether there is a valid {@link PermissionUser} object associated with the specified player.
     */
    public boolean doesUserExist(UUID uuid);
    
    /**
     * @param name the name of the player to be checked for existence of a {@link PermissionUser} object.
     * @return whether there is a valid {@link PermissionUser} object associated with the specified player.
     */
    public boolean doesUserExist(String name);
}