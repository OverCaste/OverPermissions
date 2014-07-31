package com.overmc.overpermissions.internal.wildcardsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.PermissionRemovedExecutor;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Preconditions;

public class HalfWildcardPermissible extends PermissibleBase {
    private Permissible parent = this;
    private final List<PermissionAttachment> attachments = new ArrayList<>();
    private final NodeTree<PermissionAttachmentInfo> nodeTree = new NodeTree<>();

    public HalfWildcardPermissible(ServerOperator opable) {
        super(opable);

        if (opable instanceof Permissible) {
            this.parent = (Permissible) opable;
        }

    }

    @Override
    public void recalculatePermissions( ) { // These 3 shamelessly stolen from the parental source.
        clearPermissions();
        Set<Permission> defaults = Bukkit.getServer().getPluginManager().getDefaultPermissions(isOp());
        Bukkit.getServer().getPluginManager().subscribeToDefaultPerms(isOp(), parent);

        for (Permission perm : defaults) {
            String name = perm.getName().toLowerCase();
            nodeTree.set(name, new PermissionAttachmentInfo(parent, name, null, true));
            Bukkit.getServer().getPluginManager().subscribeToPermission(name, parent);
            calculateChildPermissions(perm.getChildren(), false, null);
        }

        for (PermissionAttachment attachment : attachments) {
            calculateChildPermissions(attachment.getPermissions(), false, attachment);
        }
    }

    @Override
    public synchronized void clearPermissions( ) { // ^
        for (String name : nodeTree) {
            Bukkit.getServer().getPluginManager().unsubscribeFromPermission(name, parent);
        }

        Bukkit.getServer().getPluginManager().unsubscribeFromDefaultPerms(false, parent);
        Bukkit.getServer().getPluginManager().unsubscribeFromDefaultPerms(true, parent);

        nodeTree.clear();
    }

    private void calculateChildPermissions(Map<String, Boolean> children, boolean invert, PermissionAttachment attachment) { // ^
        Set<String> keys = children.keySet();

        for (String name : keys) {
            Permission perm = Bukkit.getServer().getPluginManager().getPermission(name);
            boolean value = children.get(name) ^ invert;
            String lname = name.toLowerCase();

            nodeTree.set(lname, new PermissionAttachmentInfo(parent, lname, attachment, value));
            Bukkit.getServer().getPluginManager().subscribeToPermission(name, parent);

            if (perm != null) {
                calculateChildPermissions(perm.getChildren(), !value, attachment);
            }
        }
    }

    @Override
    public boolean isPermissionSet(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        return nodeTree.has(permissionNode);
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        Preconditions.checkNotNull(permission, "permission");
        return isPermissionSet(permission.getName());
    }

    @Override
    public boolean hasPermission(String permissionNode) {
        Preconditions.checkNotNull(permissionNode, "permission node");
        if (isPermissionSet(permissionNode)) {
            return nodeTree.get(permissionNode).getValue();
        }
        Permission defaultPerm = Bukkit.getServer().getPluginManager().getPermission(permissionNode);
        if (defaultPerm != null) {
            return defaultPerm.getDefault().getValue(isOp());
        }
        return Permission.DEFAULT_PERMISSION.getValue(isOp());
    }

    @Override
    public boolean hasPermission(Permission permission) {
        Preconditions.checkNotNull(permission, "permission");
        return hasPermission(permission.getName());
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String permissionNode, boolean value) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkArgument(plugin.isEnabled(), "plugin " + plugin.getDescription().getName() + " is disabled");
        PermissionAttachment attachment = addAttachment(plugin);
        attachment.setPermission(permissionNode, value);
        recalculatePermissions();
        return attachment;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkArgument(plugin.isEnabled(), "plugin " + plugin.getDescription().getName() + " is disabled");
        PermissionAttachment attachment = new PermissionAttachment(plugin, parent);
        attachments.add(attachment);
        recalculatePermissions();
        return attachment;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String permissionNode, boolean value, int ticks) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(permissionNode, "permission node");
        Preconditions.checkArgument(plugin.isEnabled(), "plugin " + plugin.getDescription().getName() + " is disabled");
        PermissionAttachment attachment = addAttachment(plugin, ticks);
        if (attachment != null) {
            attachment.setPermission(permissionNode, value);
        }
        return attachment;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkArgument(plugin.isEnabled(), "plugin " + plugin.getDescription().getName() + " is disabled");
        final PermissionAttachment attachment = addAttachment(plugin);
        if (Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run( ) {
                attachment.remove();
            }
        }, ticks) == -1) {
            Bukkit.getServer().getLogger().warning("Could not add PermissionAttachment to " + this.parent + " for plugin " + plugin.getDescription().getFullName() + ": Scheduler returned -1");
            attachment.remove();
            return null;
        }
        return attachment;
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        Preconditions.checkNotNull(attachment, "attachment");

        if (this.attachments.contains(attachment)) {
            this.attachments.remove(attachment);
            PermissionRemovedExecutor ex = attachment.getRemovalCallback();

            if (ex != null) {
                ex.attachmentRemoved(attachment);
            }

            recalculatePermissions();
        } else {
            throw new IllegalArgumentException("Given attachment is not part of Permissible object " + this.parent);
        }
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions( ) {
        // TODO Auto-generated method stub
        return null;
    }
}
