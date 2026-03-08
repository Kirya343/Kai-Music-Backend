package org.kirya343.core.user.permission;

import org.kirya343.datasource.model.user.permission.Permission;
import org.kirya343.datasource.model.user.permission.Role;
import org.kirya343.dto.user.PermissionDTO;
import org.kirya343.dto.user.RoleDTO;

public interface PermissionMappingService {
    
    PermissionDTO toDTO(Permission perm);
    RoleDTO toDTO(Role role);
}