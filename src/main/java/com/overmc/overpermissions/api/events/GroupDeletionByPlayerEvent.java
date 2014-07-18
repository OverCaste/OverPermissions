package com.overmc.overpermissions.api.events;

import org.bukkit.entity.Player;

public class GroupDeletionByPlayerEvent extends GroupDeletionEvent {
    private final Player player;

    public GroupDeletionByPlayerEvent(String groupName, Player player) {
        super(groupName);
        this.player = player;
    }

    /**
     * @return the player responsible for the specified group being deleted.
     */
    public Player getPlayer( ) {
        return player;
    }
}
