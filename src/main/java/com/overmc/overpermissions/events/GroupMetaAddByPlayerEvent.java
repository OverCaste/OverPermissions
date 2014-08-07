package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;

public class GroupMetaAddByPlayerEvent extends GroupMetaAddEvent {
    private final Player player;

    public GroupMetaAddByPlayerEvent(String playerName, String worldName, String key, String value, Player player) {
        super(playerName, worldName, key, value);
        this.player = player;
    }

    public Player getPlayer( ) {
        return player;
    }

}
