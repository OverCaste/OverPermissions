package com.overmc.overpermissions;

import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;

public class KickOnFailListener implements Listener {
    final OverPermissions plugin;

    private final boolean kickOnFailure;

    public KickOnFailListener(OverPermissions plugin) {
        this.plugin = plugin;
        kickOnFailure = plugin.getConfig().getBoolean("kick-on-failure", true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void handlePlayerJoin(PlayerJoinEvent e) {
        if (kickOnFailure && plugin.checkFailure()) {
            e.getPlayer().kickPlayer("OverPermissions failed to initialize. Please report this to the staff.");
            return;
        }
    }
}
