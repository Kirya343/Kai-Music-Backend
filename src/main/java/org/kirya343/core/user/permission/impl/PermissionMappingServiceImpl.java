package org.kirya343.core.user.permission.impl;

import org.kirya343.core.user.permission.PermissionMappingService;
import org.kirya343.datasource.model.user.permission.Permission;
import org.kirya343.datasource.model.user.permission.Role;
import org.kirya343.dto.user.PermissionDTO;
import org.kirya343.dto.user.RoleDTO;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionMappingServiceImpl implements PermissionMappingService{
    
    public PermissionDTO toDTO(Permission perm) {
        return new PermissionDTO(perm.getId(), perm.getName(), perm.getComment());
    }

    public RoleDTO toDTO(Role role) {
        return new RoleDTO(role.getId(), role.getName(), role.getLevel());
    }
}
