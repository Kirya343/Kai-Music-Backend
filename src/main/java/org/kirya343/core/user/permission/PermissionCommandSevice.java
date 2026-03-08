package org.kirya343.core.user.permission;

import org.kirya343.datasource.model.user.permission.Permission;
import org.kirya343.datasource.model.user.permission.Role;

public interface PermissionCommandSevice {

    void updateRolePermission(Long roleId, Long permissionId, boolean enabled);
    Role createRole(String roleName);
    Permission createPermisson(String permissionName);
}   
