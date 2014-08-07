package com.overmc.overpermissions.permissiontests;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.UUID;

import org.bukkit.plugin.Plugin;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.*;
import com.overmc.overpermissions.internal.localentities.LocalGroup;
import com.overmc.overpermissions.internal.localentities.LocalUser;

public class MetadataInheritanceTests {
    @Test
    public void testMetadataPriority( ) {
        TemporaryPermissionManager tempManager = mock(TemporaryPermissionManager.class);
        Plugin plugin = mock(Plugin.class);

        GroupDataSource groupOneSource = mock(GroupDataSource.class);
        GroupDataSource groupTwoSource = mock(GroupDataSource.class);
        UserDataSource userSource = mock(UserDataSource.class);
        PermissionEntityDataSource worldDataSource = mock(PermissionEntityDataSource.class);
        when(worldDataSource.getMetadata()).thenReturn(Collections.<String, String> emptyMap());

        when(groupOneSource.getPriority()).thenReturn(100);
        when(groupTwoSource.getPriority()).thenReturn(10);
        when(groupOneSource.createWorldDataSource(anyString())).thenReturn(worldDataSource);
        when(groupTwoSource.createWorldDataSource(anyString())).thenReturn(worldDataSource);
        when(userSource.createWorldDataSource(anyString())).thenReturn(worldDataSource);

        LocalGroup parent = new LocalGroup(groupOneSource, tempManager, "parent", 100, true);
        LocalGroup subParent = new LocalGroup(groupTwoSource, tempManager, "subparent", 10, true);
        LocalUser user = new LocalUser(UUID.nameUUIDFromBytes("Name".getBytes(Charsets.UTF_8)), plugin, tempManager, userSource, true);
        parent.addParent(subParent);
        user.addParent(parent);

        assertNull("User must return null for invalid metadata key", user.getMeta("mykey", "world"));
        subParent.setGlobalMeta("mykey", "globalsubparent");
        assertEquals("User must properly resolve superparent's global metadata", "globalsubparent", user.getMeta("mykey", "world"));
        parent.setGlobalMeta("mykey", "globalparent");
        assertEquals("User must properly resolve parent's global metadata with inheritance.", "globalparent", user.getMeta("mykey", "world"));
        subParent.setMeta("mykey", "worldsubparent", "world");
        assertEquals("User must properly resolve superparent's world metadata.", "worldsubparent", user.getMeta("mykey", "world"));
        parent.setMeta("mykey", "worldparent", "world");
        assertEquals("User must properly resolve parent's world metadata with inheritance.", "worldparent", user.getMeta("mykey", "world"));
        user.setGlobalMeta("mykey", "global");
        assertEquals("User must properly resolve global metadata before group metadata.", "global", user.getMeta("mykey", "world"));
        user.setMeta("mykey", "world", "world");
        assertEquals("User must properly resolve global metadata before group metadata.", "world", user.getMeta("mykey", "world"));
    }
}
