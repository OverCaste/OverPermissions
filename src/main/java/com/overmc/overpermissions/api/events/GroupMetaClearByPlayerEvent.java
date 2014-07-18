package com.overmc.overpermissions.api.events;

import org.bukkit.entity.Player;

public class GroupMetaClearByPlayerEvent extends GroupMetaClearEvent {
    private final Player player;

    public GroupMetaClearByPlayerEvent(String groupName, String worldName, String key, Player player) {
        super(groupName, worldName, key);
        this.player = player;
    }

    public Player getPlayer( ) {
        return player;
    }
}
