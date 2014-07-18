package com.overmc.overpermissions.api.events;

import org.bukkit.entity.Player;

public class PlayerMetaAddByPlayerEvent extends PlayerMetaAddEvent {
    private final Player adder;

    public PlayerMetaAddByPlayerEvent(String playerName, String worldName, String key, String value, Player adder) {
        super(playerName, worldName, key, value);
        this.adder = adder;
    }

    public Player getAdder( ) {
        return adder;
    }
}
