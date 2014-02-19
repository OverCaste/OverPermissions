package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;

import com.overmc.overpermissions.PermissionChangeCause;

public class PlayerMetaAddByPlayerEvent extends PlayerMetaAddEvent {

	private final Player adder;

	public PlayerMetaAddByPlayerEvent(Player who, String meta, String value, Player adder) {
		super(who, meta, value, PermissionChangeCause.PLAYER);
		this.adder = adder;
	}

	public Player getRemover( ) {
		return adder;
	}
}
