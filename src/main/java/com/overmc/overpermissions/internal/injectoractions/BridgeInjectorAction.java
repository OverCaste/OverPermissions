package com.overmc.overpermissions.internal.injectoractions;

import com.overmc.overpermissions.api.PermissionUser;
import com.overmc.overpermissions.api.UserManager;
import com.overmc.overpermissions.exceptions.StartException;
import com.overmc.overpermissions.internal.bukkitclasses.PermissibleBaseUserBridge;
import com.overmc.overpermissions.internal.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

public class BridgeInjectorAction implements WildcardAction {
    private final Class<?> craftHumanEntityClass;
    private final Field permField;

    private final UserManager userManager;

    public BridgeInjectorAction(UserManager userManager) {
        this.userManager = userManager;
        try {
            craftHumanEntityClass = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftHumanEntity");

            permField = craftHumanEntityClass.getDeclaredField("perm");
            permField.setAccessible(true);
            ReflectionUtils.setFieldModifiable(permField);
        } catch (ClassNotFoundException e) {
            throw new StartException("The option 'injection-mode' is enabled, but the server implementation wasn't CraftBukkit!");
        } catch (NoSuchFieldException e) {
            throw new StartException("The option 'injection-mode' is enabled, but the CraftBukkit implementation wasn't compatible!");
        } catch (SecurityException e) {
            throw new StartException("There was a security exception thrown while trying to inject OverPermissions classes into CraftBukkit!");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    public void injectBridge(Player p) {
        try {
            PermissibleBase playerPermissibleBase = (PermissibleBase) permField.get(p);
            permField.set(p, new PermissibleBaseUserBridge(p, new UserFetcher(p.getName())));
            playerPermissibleBase.recalculatePermissions();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initializePlayer(Player p) {
        injectBridge(p);
    }

    @Override
    public void deinitializePlayer(Player p) {
        // Do nothing
    }

    private class UserFetcher implements Callable<PermissionUser> {
        private final String username;

        public UserFetcher(String username) {
            this.username = username;
        }

        @Override
        public PermissionUser call( ) throws Exception {
            return userManager.getPermissionUser(username);
        }
    }
}