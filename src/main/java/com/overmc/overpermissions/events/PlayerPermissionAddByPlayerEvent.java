package com.overmc.overpermissions.events;

import org.bukkit.entity.Player;

import com.overmc.overpermissions.PermissionChangeCause;

public class PlayerPermissionAddByPlayerEvent extends PlayerPermissionAddEvent {

	private final Player adder;

	public PlayerPermissionAddByPlayerEvent(String playerName, String worldName, String node, Player adder) {
		super(playerName, worldName, node, PermissionChangeCause.PLAYER);
		this.adder = adder;
	}

	public Player getAdder( ) {
		return adder;
	}

}
