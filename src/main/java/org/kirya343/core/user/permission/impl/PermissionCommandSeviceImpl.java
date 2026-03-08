package org.kirya343.core.user.permission.impl;

import org.kirya343.core.user.permission.PermissionCommandSevice;
import org.kirya343.datasource.model.user.permission.Permission;
import org.kirya343.datasource.model.user.permission.Role;
import org.kirya343.datasource.repository.user.permission.PermissionRepository;
import org.kirya343.datasource.repository.user.permission.RoleRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionCommandSeviceImpl implements PermissionCommandSevice {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public void updateRolePermission(Long roleId, Long permissionId, boolean enabled) {

        if (enabled) {
            roleRepository.addPermissionToRole(roleId, permissionId);
        } else {
            roleRepository.removePermissionFromRole(roleId, permissionId);
        }
    }

    public Role createRole(String roleName) {
        Role role = new Role(roleName, 0);
        return roleRepository.save(role);
    }

    public Permission createPermisson(String permissionName) {
        Permission perm = new Permission(permissionName);
        return permissionRepository.save(perm);
    }
}

