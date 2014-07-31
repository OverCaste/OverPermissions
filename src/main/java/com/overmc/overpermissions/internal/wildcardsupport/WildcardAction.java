package com.overmc.overpermissions.internal.wildcardsupport;

import org.bukkit.entity.Player;

public interface WildcardAction {
    public void initializePlayer(Player p);

    public void deinitializePlayer(Player p);
}
