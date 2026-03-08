package org.kirya343.core.user.permission;

import java.util.List;

import org.kirya343.dto.user.PermissionDTO;
import org.kirya343.dto.user.RoleDTO;
import org.springframework.lang.NonNull;

public interface PermissionQueryService {
    List<RoleDTO> getAllRoleDtos();
    List<PermissionDTO> getAllPermissionDtos();
    List<PermissionDTO> getPermissionDtosByRole(@NonNull Long roleId);
}