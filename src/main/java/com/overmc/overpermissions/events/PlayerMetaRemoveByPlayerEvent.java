package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;

public class PlayerMetaRemoveByPlayerEvent extends PlayerMetaRemoveEvent {
    private final Player remover;

    public PlayerMetaRemoveByPlayerEvent(String playerName, String worldName, String key, Player remover) {
        super(playerName, worldName, key);
        this.remover = remover;
    }

    public Player getRemover( ) {
        return remover;
    }
}
