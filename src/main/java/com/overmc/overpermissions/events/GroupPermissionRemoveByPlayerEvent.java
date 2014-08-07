package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;

public class GroupPermissionRemoveByPlayerEvent extends GroupPermissionRemoveEvent {
    private final Player player;

    public GroupPermissionRemoveByPlayerEvent(String groupName, String node, String worldName, Player player) {
        super(groupName, node, worldName);
        this.player = player;
    }

    public GroupPermissionRemoveByPlayerEvent(String groupName, String node, String worldName, boolean temporary, Player player) {
        super(groupName, node, worldName, temporary);
        this.player = player;
    }

    public Player getPlayer( ) {
        return player;
    }
}
