package com.overmc.overpermissions.permissiontsts;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.bukkit.plugin.Plugin;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.overmc.overpermissions.internal.TemporaryPermissionManager;
import com.overmc.overpermissions.internal.datasources.GroupDataSource;
import com.overmc.overpermissions.internal.datasources.UserDataSource;
import com.overmc.overpermissions.internal.localentities.LocalGroup;
import com.overmc.overpermissions.internal.localentities.LocalUser;

public final class PermissionInheritanceTests {
    @Test
    public void testInheritanceAutoUpdate( ) {
        TemporaryPermissionManager tempManager = mock(TemporaryPermissionManager.class);
        Plugin plugin = mock(Plugin.class);
        
        GroupDataSource groupOneSource = mock(GroupDataSource.class);
        GroupDataSource groupTwoSource = mock(GroupDataSource.class);
        when(groupOneSource.getPriority()).thenReturn(100);
        when(groupTwoSource.getPriority()).thenReturn(10);
        UserDataSource userSource = mock(UserDataSource.class);
        
        LocalGroup parent = new LocalGroup(groupOneSource, tempManager, "parent",  2, true);
        LocalGroup subParent = new LocalGroup(groupTwoSource, tempManager, "subparent",  1, true);
        LocalUser user = new LocalUser(UUID.nameUUIDFromBytes("Name".getBytes(Charsets.UTF_8)), plugin, tempManager, userSource, true);
        
        user.addGlobalPermissionNode("my.node.*");
        
        assertTrue("user must match wildcard literals.", user.hasGlobalPermission("my.node.*"));
        assertTrue("user must match sub-wildcard literals.", user.hasGlobalPermission("my.node.subnode"));
        assertTrue("user must match sub-sub-wildcard literals.", user.hasGlobalPermission("my.node.subnode.subsubnode"));
        
        user.addParent(parent);
        parent.addUserToGroup(user);
        assertFalse("user must not match an unadded node.", user.hasGlobalPermission("parentnode"));
        parent.addGlobalPermissionNode("parentnode");
        assertTrue("user must have a node from a direct parent without manually recalculating permissions.", user.hasGlobalPermission("parentnode"));
        assertFalse("user must not match an unadded node.", user.hasGlobalPermission("subparentnode"));
        
        subParent.addGlobalPermissionNode("subparentnode");
        parent.addParent(subParent);
        assertTrue("user must have a node from an indirect parent without manually recalculating permissions.", user.hasGlobalPermission("subparentnode"));
        
        subParent.addGlobalPermissionNode("subparentothernode");
        assertTrue("user must have a node from an indirect parent without being refreshed by a parent being added.", user.hasGlobalPermission("subparentothernode"));
    }
    
    @Test
    public void testPermissionPriority( ) {
        TemporaryPermissionManager tempManager = mock(TemporaryPermissionManager.class);
        Plugin plugin = mock(Plugin.class);
        
        GroupDataSource groupOneSource = mock(GroupDataSource.class);
        GroupDataSource groupTwoSource = mock(GroupDataSource.class);
        when(groupOneSource.getPriority()).thenReturn(100);
        when(groupTwoSource.getPriority()).thenReturn(10);
        UserDataSource userSource = mock(UserDataSource.class);
        
        LocalGroup parent = new LocalGroup(groupOneSource, tempManager, "parent",  2, true);
        LocalGroup subParent = new LocalGroup(groupTwoSource, tempManager, "subparent",  1, true);
        LocalUser user = new LocalUser(UUID.nameUUIDFromBytes("Name".getBytes(Charsets.UTF_8)), plugin, tempManager, userSource, true);
        parent.addParent(subParent);
        user.addParent(parent);
        assertFalse("User must not have a global permission by default.", user.hasGlobalPermission("mynode"));
        subParent.addGlobalPermissionNode("mynode");
        assertTrue("User must have a global permission if nothing is negating it.", user.hasGlobalPermission("mynode"));
        parent.addGlobalPermissionNode("-mynode");
        assertFalse("User must not have a global permission that is forced to false.", user.hasGlobalPermission("mynode"));
        user.addGlobalPermissionNode("+mynode");
        assertTrue("User must have a global permission if it is forced to true.", user.hasGlobalPermission("mynode"));
    }
}
