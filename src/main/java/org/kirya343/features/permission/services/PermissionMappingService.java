package org.kirya343.features.permission.services;

import org.kirya343.features.permission.datasource.model.Permission;
import org.kirya343.features.permission.datasource.model.Role;
import org.kirya343.features.permission.dto.PermissionDTO;
import org.kirya343.features.permission.dto.RoleDTO;

public interface PermissionMappingService {
    
    PermissionDTO toDTO(Permission perm);
    RoleDTO toDTO(Role role);
}