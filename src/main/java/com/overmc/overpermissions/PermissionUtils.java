package com.overmc.overpermissions;

import java.util.*;

public class PermissionUtils {
    public static HashMap<String, Boolean> getPermissionValues(List<String> permissions) {
        HashMap<String, Boolean> ret = new HashMap<String, Boolean>();
        for (String permission : permissions) {
            if ((permission == null) || ((permission.length() == 1) && (permission.startsWith("-") || permission.startsWith("+")))) {
                continue;
            }
            permission = permission.toLowerCase();
            if (permission.startsWith("-")) {
                ret.put(permission.substring(1).toLowerCase(), false);
            } else {
                if (permission.startsWith("+")) { // regardless, +perm will ALWAYS be true
                    ret.put(permission.substring(1).toLowerCase(), true);
                } else {
                    if (!permissions.contains("-" + permission)) { // doesn't contain neg
                        ret.put(permission, true);
                    }
                }
            }
        }
        return ret;
    }
}
