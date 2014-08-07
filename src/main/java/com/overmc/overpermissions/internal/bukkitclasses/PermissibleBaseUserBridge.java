package com.overmc.overpermissions.internal.bukkitclasses;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.*;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.overmc.overpermissions.api.PermissionGroup;
import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.internal.util.ReflectionUtils;

public class PermissibleBaseUserBridge extends PermissibleBase {
    private final Player player;
    private final Callable<PermissionUser> userFetcher;

    public PermissibleBaseUserBridge(Player player, Callable<PermissionUser> userFetcher) {
        super(player);
        this.player = player;
        this.userFetcher = userFetcher;
        try {
            Field f = PermissibleBase.class.getDeclaredField("permissions");
            ReflectionUtils.setFieldModifiable(f);
            f.set(this, null); // We won't be using this.
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ExceptionInInitializerError e) {
            e.printStackTrace();
        }
    }

    protected PermissionUser getUser( ) {
        try {
            return userFetcher.call();
        } catch (Exception e) {
            throw new RuntimeException(e); // This should never happen in any reasonable implementation.
        }
    }

    @Override
    public boolean isPermissionSet(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        return getUser().hasPermission(permission, player.getWorld().getName());
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        Preconditions.checkNotNull(permission, "permission");
        return isPermissionSet(permission.getName());
    }

    @Override
    public boolean hasPermission(String permission) {
        Preconditions.checkNotNull(permission, "permission");
        String lowercasePermission = permission.toLowerCase();
        if (isPermissionSet(lowercasePermission)) {
            return getUser().getPermission(lowercasePermission, player.getWorld().getName());
        }
        Permission perm = Bukkit.getServer().getPluginManager().getPermission(lowercasePermission);
        if (perm != null) {
            return perm.getDefault().getValue(isOp());
        }
        return Permission.DEFAULT_PERMISSION.getValue(isOp());
    }

    @Override
    public boolean hasPermission(Permission permission) {
        Preconditions.checkNotNull(permission, "permission");
        String lowercasePermission = permission.getName().toLowerCase();

        if (isPermissionSet(lowercasePermission)) {
            return getUser().getPermission(lowercasePermission, player.getWorld().getName());
        }
        return permission.getDefault().getValue(isOp());
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String permission, boolean value) {
        throw new UnsupportedOperationException("Can't add attachments to an OverPermissions Bridge! Ensure you only have one permission plugin installed!");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        throw new UnsupportedOperationException("Can't add attachments to an OverPermissions Bridge! Ensure you only have one permission plugin installed!");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        throw new UnsupportedOperationException("Can't add attachments to an OverPermissions Bridge! Ensure you only have one permission plugin installed!");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        throw new UnsupportedOperationException("Can't add attachments to an OverPermissions Bridge! Ensure you only have one permission plugin installed!");
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        throw new UnsupportedOperationException("Can't remove attachments from an OverPermissions Bridge! Ensure you only have one permission plugin installed!");
    }

    @Override
    public void clearPermissions( ) {
        // Do nothing
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions( ) {
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
        return ret;
    }
}
