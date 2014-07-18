package com.overmc.overpermissions.api.events;

import org.bukkit.entity.Player;

public class GroupCreationByPlayerEvent extends GroupCreationEvent {
    private final Player player;

    public GroupCreationByPlayerEvent(String groupName, Player player) {
        super(groupName);
        this.player = player;
    }

    /**
     * @return the player responsible for the specified group being created.
     */
    public Player getPlayer( ) {
        return player;
    }
}
