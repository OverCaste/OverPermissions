package com.overmc.overpermissions.internal.injectoractions;

import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.plugin.Plugin;

import com.overmc.overpermissions.exceptions.StartException;
import com.overmc.overpermissions.internal.NodeTree;
import com.overmc.overpermissions.internal.util.ReflectionUtils;

public class HalfWildcardPlayerInjectorAction implements WildcardAction {
    private final Class<?> craftHumanEntityClass;
    private final Field permField;
    private final Field permNodesField;

    public HalfWildcardPlayerInjectorAction(Plugin plugin) {
        try {
            craftHumanEntityClass = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftHumanEntity");

            permField = craftHumanEntityClass.getDeclaredField("perm");
            permField.setAccessible(true);

            permNodesField = PermissibleBase.class.getDeclaredField("permissions");
            ReflectionUtils.setFieldModifiable(permNodesField);
            permNodesField.setAccessible(true);

        } catch (ClassNotFoundException e) {
            throw new StartException("The option 'wildcard-support' is enabled, but the server implementation wasn't compatible!");
        } catch (NoSuchFieldException e) {
            throw new StartException("The option 'wildcard-support' is enabled, but the Bukkit implementation wasn't compatible!");
        } catch (SecurityException e) {
            throw new StartException("There was a security exception thrown while trying to inject Wildcard support into Bukkit!");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    public void InjectHalfWildcardCompatibility(Player p) {
        try {
            PermissibleBase playerPermissibleBase = (PermissibleBase) permField.get(p);
            permNodesField.set(playerPermissibleBase, new NodeTree<>());
            playerPermissibleBase.recalculatePermissions();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initializePlayer(Player p) {
        InjectHalfWildcardCompatibility(p);
    }

    @Override
    public void deinitializePlayer(Player p) {
        // Do nothing
    }
}