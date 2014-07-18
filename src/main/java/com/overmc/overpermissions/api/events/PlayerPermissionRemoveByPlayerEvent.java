package com.overmc.overpermissions.api.events;

import org.bukkit.entity.Player;

public class PlayerPermissionRemoveByPlayerEvent extends PlayerPermissionRemoveEvent {
    private final Player remover;

    public PlayerPermissionRemoveByPlayerEvent(String playerName, String worldName, String node, boolean temporary, Player remover) {
        super(playerName, worldName, node, temporary);
        this.remover = remover;
    }
    
    public PlayerPermissionRemoveByPlayerEvent(String playerName, String worldName, String node, Player remover) {
        super(playerName, worldName, node);
        this.remover = remover;
    }
    
    public Player getRemover( ) {
        return remover;
    }
}
