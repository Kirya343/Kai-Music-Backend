package org.kirya343.features.permission.services;

import org.kirya343.features.permission.datasource.model.Permission;
import org.kirya343.features.permission.datasource.model.Role;

public interface PermissionCommandSevice {

    void updateRolePermission(Long roleId, Long permissionId, boolean enabled);
    Role createRole(String roleName);
    Permission createPermisson(String permissionName);
}   
