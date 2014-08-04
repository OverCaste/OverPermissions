package com.overmc.overpermissions.internal.injectoractions;

import org.bukkit.entity.Player;

public interface WildcardAction {
    public void initializePlayer(Player p);

    public void deinitializePlayer(Player p);
}
