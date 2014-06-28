package com.overmc.overpermissions;

import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class GeneralListener implements Listener {
    final OverPermissions plugin;

    public GeneralListener(OverPermissions plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handlePlayerJoin(PlayerJoinEvent e) {
        plugin.initPlayer(e.getPlayer());
    }

    @EventHandler
    public void handlePlayerQuit(PlayerQuitEvent e) {
        plugin.deinitPlayer(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void handlePlayerChangedWorld(PlayerChangedWorldEvent e) {
        PlayerPermissionData pd = plugin.getPlayerPermissions(e.getPlayer());
        pd.recalculateMeta();
        pd.recalculatePermissions();
    }
}
