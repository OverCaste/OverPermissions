package com.overmc.overpermissions.api;

import java.util.Set;

public interface PermissionUser extends PermissionEntity, MetadataEntity, TemporaryPermissionEntity {
    /**
     * Get a list of all parental groups inherited by this user, including recursive parents.
     * 
     * @return an immutable, data-backed set of every group inherited by this user.
     */
    public Set<? extends PermissionGroup> getAllParents( );
    
    /**
     * Get a list of the direct group parents of this user.
     * 
     * @return an immutable, data-backed set of the direct groups of this user.
     */
    public Set<? extends PermissionGroup>  getParents( );

    /**
     * Adds a parent to this user.
     * 
     * @param parent the parental group to be added.
     * @return whether the specified parent didn't exist and was added.
     */
    public boolean addParent(PermissionGroup parent);

    /**
     * Removes a parent from this user.
     * 
     * @param parent the parental group to be removed.
     * @return whether the specified parent existed and was removed.
     */
    public boolean removeParent(PermissionGroup parent);
    
    /**
     * @return whether this user already exists internally. If it doesn't exist and you call an operation that changes the state, the user is created.
     */
    public boolean exists( );
    
    /**
     * @return the unique name of this permission user.
     */
    public String getName( );
}
