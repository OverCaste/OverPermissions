package com.overmc.overpermissions;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class ServerManager implements PluginMessageListener {
	private final OverPermissions plugin;
	private int serverId;

	public ServerManager(OverPermissions plugin) {
		this.plugin = plugin;
	}

	public void init( ) {
		this.plugin.getServer().getMessenger().registerOutgoingPluginChannel(this.plugin, "BungeeCord");
		this.plugin.getServer().getMessenger().registerIncomingPluginChannel(this.plugin, "BungeeCord", this);
		this.serverId = this.plugin.getConfig().getInt("server-id", -1);
		if (this.serverId == -1) {
			this.serverId = this.plugin.getSQLManager().getNewServerId();
			this.plugin.getConfig().set("server-id", this.serverId);
			this.plugin.saveConfig();
		}
	}

	public int getServerId( ) {
		return this.serverId;
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] data) {

	}
}
