package org.kirya343.features.permission.services.impl;

import org.kirya343.features.permission.services.PermissionMappingService;
import org.kirya343.features.permission.datasource.model.Permission;
import org.kirya343.features.permission.datasource.model.Role;
import org.kirya343.features.permission.dto.PermissionDTO;
import org.kirya343.features.permission.dto.RoleDTO;
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
