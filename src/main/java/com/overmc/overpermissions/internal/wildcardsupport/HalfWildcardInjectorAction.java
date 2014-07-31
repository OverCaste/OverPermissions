package com.overmc.overpermissions.internal.wildcardsupport;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.overmc.overpermissions.internal.StartException;

public class HalfWildcardInjectorAction implements WildcardAction {
    private final Class<?> craftHumanEntityClass;
    private final Field permField;
    private final Field modifiersField;

    public HalfWildcardInjectorAction(Plugin plugin) {
        try {
            craftHumanEntityClass = Class.forName(Bukkit.getServer().getClass().getPackage() + ".entity.CraftHumanEntity");
            permField = craftHumanEntityClass.getDeclaredField("perm");
            modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.set(permField, permField.getModifiers() & ~Modifier.FINAL);
            permField.setAccessible(true);
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
            permField.set(p, new HalfWildcardPermissible(p));
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