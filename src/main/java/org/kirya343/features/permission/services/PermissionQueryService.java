package org.kirya343.features.permission.services;

import java.util.List;

import org.kirya343.features.permission.dto.PermissionDTO;
import org.kirya343.features.permission.dto.RoleDTO;
import org.springframework.lang.NonNull;

public interface PermissionQueryService {
    List<RoleDTO> getAllRoleDtos();
    List<PermissionDTO> getAllPermissionDtos();
    List<PermissionDTO> getPermissionDtosByRole(@NonNull Long roleId);
}