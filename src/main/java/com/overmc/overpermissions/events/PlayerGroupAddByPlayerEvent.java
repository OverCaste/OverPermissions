package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;

public class PlayerGroupAddByPlayerEvent extends PlayerGroupAddEvent {
    private final Player player;

    public PlayerGroupAddByPlayerEvent(String playerName, String group, Player player) {
        super(playerName, group);
        this.player = player;
    }

    @Override
    public Player getPlayer( ) {
        return player;
    }
}
