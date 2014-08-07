package com.overmc.overpermissions.events;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;

import com.google.common.base.Preconditions;

public abstract class GroupMetaEvent extends Event {
    private final String key;
    private final String worldName;
    private final String groupName;
    private boolean cancelled;

    public GroupMetaEvent(String groupName, String worldName, String key) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(groupName);
        this.key = key;
        this.worldName = worldName;
        this.groupName = groupName;
    }

    public String getKey( ) {
        return key;
    }

    /**
     * @return The name of the group who's permissions are being changed. Guaranteed to not return null.
     */
    public String getGroupName( ) {
        return groupName;
    }

    /**
     * @return The world of the group who's permissions are being changed. Null if the world is global.
     */
    public String getWorldName( ) {
        return worldName;
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
