package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;

import com.overmc.overpermissions.PermissionChangeCause;

public class PlayerPermissionRemoveByPlayerEvent extends PlayerPermissionRemoveEvent {

    private final Player remover;

    public PlayerPermissionRemoveByPlayerEvent(String playerName, String worldName, String node, Player remover) {
        super(playerName, worldName, node, PermissionChangeCause.PLAYER);
        this.remover = remover;
    }

    public Player getRemover( ) {
        return remover;
    }
}
