package com.overmc.overpermissions.internal.bukkitclasses;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.PermissionUser;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class PermissibleBaseUserBridge extends PermissibleBase {
    private final Player player;
    private final Callable<PermissionUser> userFetcher;

    public PermissibleBaseUserBridge(Player player, Callable<PermissionUser> userFetcher) {
        super(player);
        this.player = player;
        this.userFetcher = userFetcher;
    }

    protected PermissionUser getUser() {
        try {
            return userFetcher.call();
        } catch (Exception e) {
            throw new RuntimeException(e); // This should never happen in any reasonable implementation.
        }
    }

    @Override
    public boolean isPermissionSet(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        return getUser().hasPermission(permission, player.getWorld().getName()) || super.isPermissionSet(permission);
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        Preconditions.checkNotNull(permission, "permission");
        return isPermissionSet(permission.getName()) || super.isPermissionSet(permission);
    }

    @Override
    public boolean hasPermission(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        String lowercasePermission = permission.toLowerCase();
        //Check overpermissions
        if (getUser().hasPermission(lowercasePermission, player.getWorld().getName())) {
            return getUser().getPermission(lowercasePermission, player.getWorld().getName());
        }
        //Check bukkit permissions
        return super.hasPermission(permission);
    }

    @Override
    public boolean hasPermission(Permission permission) {
        Preconditions.checkNotNull(permission, "permission");
        return hasPermission(permission.getName());
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        PermissionUser user = getUser();
        Set<PermissionAttachmentInfo> ret = new HashSet<>();
        Map<String, Boolean> values = Maps.newHashMap();
        for (Map.Entry<String, Boolean> entry : user.getPermissionValues(player.getWorld().getName()).entrySet()) {
            values.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Boolean> entry : user.getGlobalPermissionValues().entrySet()) {
            values.put(entry.getKey(), entry.getValue());
        }
        for (PermissionGroup g : user.getAllParents()) { // This is guaranteed natural ordering.
            for (Map.Entry<String, Boolean> entry : g.getPermissionValues(player.getWorld().getName()).entrySet()) {
                values.put(entry.getKey(), entry.getValue());
            }
        }
        for (PermissionGroup g : user.getAllParents()) { // This is guaranteed natural ordering.
            for (Map.Entry<String, Boolean> entry : g.getGlobalPermissionValues().entrySet()) {
                values.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, Boolean> entry : values.entrySet()) {
            ret.add(new PermissionAttachmentInfo(player, entry.getKey(), null, entry.getValue()));
        }
        ret.addAll(super.getEffectivePermissions());
        return ret;
    }
}
