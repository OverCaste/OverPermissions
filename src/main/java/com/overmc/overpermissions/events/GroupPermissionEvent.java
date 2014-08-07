package com.overmc.overpermissions.events;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;

import com.google.common.base.Preconditions;

public abstract class GroupPermissionEvent extends Event {
    private final String node;
    private final String worldName;
    private final String groupName;
    private boolean cancelled = false;

    public GroupPermissionEvent(String groupName, String node, String worldName) {
        Preconditions.checkNotNull(groupName, "group name");
        Preconditions.checkNotNull(node, "node");
        this.node = node;
        this.worldName = worldName;
        this.groupName = groupName;
    }

    /**
     * @return the permission node being changed by this event.
     */
    public String getNode( ) {
        return node;
    }

    /**
     * @return the name of the group who's permissions are being changed.
     */
    public String getGroupName( ) {
        return groupName;
    }

    /**
     * @return true if this permission is being changed across all worlds, false otherwise.
     */
    public boolean isGlobal( ) {
        return worldName == null;
    }

    /**
     * @return null - If the world selected is global, otherwise the world in which the permission is being changed.
     * 
     * @see #getWorldName()
     */
    public World getWorld( ) {
        if (worldName == null) {
            return null;
        }
        return Bukkit.getWorld(worldName);
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled( ) {
        return cancelled;
    }
}
