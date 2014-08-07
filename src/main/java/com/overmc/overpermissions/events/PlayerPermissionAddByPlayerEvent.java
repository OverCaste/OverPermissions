package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;

public class PlayerPermissionAddByPlayerEvent extends PlayerPermissionAddEvent {
    private final Player adder;

    public PlayerPermissionAddByPlayerEvent(String playerName, String worldName, String node, Player adder) {
        super(playerName, worldName, node);
        this.adder = adder;
    }

    public PlayerPermissionAddByPlayerEvent(String playerName, String worldName, String node, boolean temporary, Player adder) {
        super(playerName, worldName, node, temporary);
        this.adder = adder;
    }

    public Player getAdder( ) {
        return adder;
    }
}
