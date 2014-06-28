package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;

import com.overmc.overpermissions.PermissionChangeCause;

public class PlayerMetaRemoveByPlayerEvent extends PlayerMetaRemoveEvent {

    private final Player adder;

    public PlayerMetaRemoveByPlayerEvent(Player who, String meta, Player adder) {
        super(who, meta, PermissionChangeCause.PLAYER);
        this.adder = adder;
    }

    public Player getAdder( ) {
        return adder;
    }
}
