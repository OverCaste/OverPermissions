package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;

public class GroupRemoveParentByPlayerEvent extends GroupRemoveParentEvent {
    private final Player player;

    public GroupRemoveParentByPlayerEvent(String groupName, String parentName, Player player) {
        super(groupName, parentName);
        this.player = player;
    }

    public Player getPlayer( ) {
        return player;
    }
}
