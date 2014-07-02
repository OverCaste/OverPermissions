package com.overmc.overpermissions.api;

import java.util.Set;

public interface PermissionGroup extends PermissionEntity, MetadataEntity, TemporaryPermissionEntity, Comparable<PermissionGroup> {
    /**
     * @return An immutable collection of this group's direct parents.
     */
    public Set<? extends PermissionGroup> getParents( );

    /**
     * @return An immutable collection of this group's parents, retrieved recursively from it's direct parents.
     * 
     * @see #getParents()
     */
    public Set<? extends PermissionGroup> getAllParents( );

    /**
     * Adds a parent to this group.
     * 
     * @param parent the parental group to be added.
     * @return whether the specified parent didn't exist and was added.
     */
    public boolean addParent(PermissionGroup parent);

    /**
     * Removes a parent from this group.
     * 
     * @param parent the parental group to be removed.
     * @return whether the specified parent existed and was removed.
     */
    public boolean removeParent(PermissionGroup parent);

    /**
     * @return An immutable collection of this group's direct children.
     */
    public Set<? extends PermissionGroup> getChildren( );

    /**
     * @return the unique name of this group.
     */
    public String getName( );

    /**
     * Retrieve the priority of this group, lower group priorities will cause those groups' nodes and metadata to be overridden by those of groups with higher priority.
     * 
     * @return the priority of this group.
     */
    public int getPriority( );
}
