package com.overmc.overpermissions.api.events;

import org.bukkit.entity.Player;

public class PlayerGroupSetByPlayerEvent extends PlayerGroupSetEvent {
    private final Player player;

    public PlayerGroupSetByPlayerEvent(String playerName, String group, Player player) {
        super(playerName, group);
        this.player = player;
    }

    @Override
    public Player getPlayer( ) {
        return player;
    }
}
