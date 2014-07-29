package com.overmc.overpermissions.api;

import java.util.Set;

public interface PermissionUser extends PermissionEntity, MetadataEntity, TemporaryPermissionEntity, TransientPermissionEntity {
    /**
     * Get a list of all parental groups inherited by this user, including recursive parents.
     * 
     * @return an immutable, data-backed set of every group inherited by this user.
     */
    public Set<PermissionGroup> getAllParents( );

    /**
     * Get a list of the direct group parents of this user.
     * 
     * @return an immutable, data-backed set of the direct groups of this user.
     */
    public Set<PermissionGroup> getParents( );

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
     * Removes all parents from this group, then sets it's only parent to the specified one.
     * 
     * @param parent the parent to be the user's exclusive group.
     */
    public void setParent(PermissionGroup parent);
}
