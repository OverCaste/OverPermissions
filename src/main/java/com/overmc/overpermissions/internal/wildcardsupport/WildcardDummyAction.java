package com.overmc.overpermissions.internal.wildcardsupport;

import org.bukkit.entity.Player;

public class WildcardDummyAction implements WildcardAction {
    @Override
    public void initializePlayer(Player p) {
        // Do nothing
    }

    @Override
    public void deinitializePlayer(Player p) {
        // Do nothing
    }
}
