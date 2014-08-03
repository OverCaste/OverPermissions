package com.overmc.overpermissions.internal.wildcardsupport;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.plugin.Plugin;

import com.overmc.overpermissions.internal.StartException;

public class HalfWildcardInjectorAction implements WildcardAction {
    private final Class<?> craftHumanEntityClass;
    private final Field permField;
    private final Field permNodesField;
    private final Field modifiersField;

    public HalfWildcardInjectorAction(Plugin plugin) {
        try {
            craftHumanEntityClass = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".entity.CraftHumanEntity");
            modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);

            permField = craftHumanEntityClass.getDeclaredField("perm");
            permField.setAccessible(true);

            permNodesField = PermissibleBase.class.getDeclaredField("permissions");
            modifiersField.set(permNodesField, permNodesField.getModifiers() & ~Modifier.FINAL);
            permNodesField.setAccessible(true);

        } catch (ClassNotFoundException e) {
            throw new StartException("The option 'wildcard-support' is enabled, but the server implementation wasn't CraftBukkit!");
        } catch (NoSuchFieldException e) {
            throw new StartException("The option 'wildcard-support' is enabled, but the CraftBukkit implementation wasn't compatible!");
        } catch (SecurityException e) {
            throw new StartException("There was a security exception thrown while trying to inject Wildcard support into CraftBukkit!");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
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