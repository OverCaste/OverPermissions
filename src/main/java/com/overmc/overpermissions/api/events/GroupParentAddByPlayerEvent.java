package com.overmc.overpermissions.api.events;

import org.bukkit.entity.Player;

public class GroupParentAddByPlayerEvent extends GroupParentAddEvent {
    private final Player player;

    public GroupParentAddByPlayerEvent(String groupName, String parentName, Player player) {
        super(groupName, parentName);
        this.player = player;
    }

    /**
     * @return the player that caused this event to be called.
     */
    public Player getPlayer( ) {
        return player;
    }
}
