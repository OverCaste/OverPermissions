package com.overmc.overpermissions.internal.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public enum ReflectionUtils { // simple singleton
    INSTANCE;

    private final Field modifiersField;

    private ReflectionUtils( ) {
        try {
            modifiersField = Field.class.getDeclaredField("modifiers");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }
        modifiersField.setAccessible(true);
    }

    private void setFieldModifiableInternal(Field f) {
        try {
            modifiersField.set(f, f.getModifiers() & ~Modifier.FINAL);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void setFieldModifiable(Field f) {
        INSTANCE.setFieldModifiableInternal(f);
    }
}
