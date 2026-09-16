package org.kirya343.features.permission.services.impl;

import org.kirya343.features.permission.services.PermissionCommandSevice;
import org.kirya343.features.permission.datasource.model.Permission;
import org.kirya343.features.permission.datasource.model.Role;
import org.kirya343.features.permission.datasource.repository.PermissionRepository;
import org.kirya343.features.permission.datasource.repository.RoleRepository;
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

