package com.overmc.overpermissions.internal.util;

import java.util.HashMap;
import java.util.Set;

public class PermissionUtils {
    public static HashMap<String, Boolean> getPermissionValues(Set<String> nodes) {
        HashMap<String, Boolean> ret = new HashMap<String, Boolean>();
        for (String permission : nodes) {
            if ((permission == null) || ((permission.length() == 1) && (permission.startsWith("-") || permission.startsWith("+")))) {
                continue;
            }
            permission = permission.toLowerCase();
            String baseNode = getBaseNode(permission);
            if (!ret.containsKey(baseNode)) {
                ret.put(baseNode, getPermissionValue(permission, baseNode, nodes));
            }
        }
        return ret;
    }

    public static boolean getPermissionValue(String permission, String baseNode, Set<String> nodes) {
        System.out.println("Permission value for " + permission + ", " + baseNode + ": " + nodes.contains("+" + baseNode) + ", " + nodes.contains("-" + baseNode) + ", " + nodes.contains(baseNode));
        if (nodes.contains("+" + baseNode)) {
            return true;
        }
        if (nodes.contains("-" + baseNode)) {
            return false;
        }
        return nodes.contains(baseNode);
    }

    public static boolean getPermissionValue(String permission, Set<String> nodes) {
        return getPermissionValue(permission, getBaseNode(permission), nodes);
    }

    public static String getBaseNode(String node) {
        return (node.startsWith("+") || node.startsWith("-")) ? node.substring(1) : node;
    }
}
