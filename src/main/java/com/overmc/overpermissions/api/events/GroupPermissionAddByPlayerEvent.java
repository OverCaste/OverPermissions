package com.overmc.overpermissions.api.events;

import org.bukkit.entity.Player;

public class GroupPermissionAddByPlayerEvent extends GroupPermissionAddEvent {
    private final Player player;

    public GroupPermissionAddByPlayerEvent(String groupName, String worldName, String permission, Player player) {
        super(groupName, permission, worldName);
        this.player = player;
    }

    public GroupPermissionAddByPlayerEvent(String groupName, String worldName, String permission, boolean temporary, Player adder) {
        super(groupName, worldName, permission, temporary);
        this.player = adder;
    }

    public Player getPlayer( ) {
        return player;
    }
}
