package com.overmc.overpermissions.api.events;

import org.bukkit.entity.Player;

public class PlayerGroupRemoveByPlayerEvent extends PlayerGroupRemoveEvent {
    private final Player player;

    public PlayerGroupRemoveByPlayerEvent(String playerName, String group, Player player) {
        super(playerName, group);
        this.player = player;
    }

    @Override
    public Player getPlayer( ) {
        return player;
    }
}
