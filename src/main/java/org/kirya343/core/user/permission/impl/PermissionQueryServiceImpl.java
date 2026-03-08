package org.kirya343.core.user.permission.impl;

import java.util.List;
import java.util.Set;

import org.kirya343.core.user.permission.PermissionMappingService;
import org.kirya343.core.user.permission.PermissionQueryService;
import org.kirya343.datasource.model.user.permission.Permission;
import org.kirya343.datasource.model.user.permission.Role;
import org.kirya343.datasource.repository.user.permission.PermissionRepository;
import org.kirya343.datasource.repository.user.permission.RoleRepository;
import org.kirya343.dto.user.PermissionDTO;
import org.kirya343.dto.user.RoleDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@Profile({"production", "statistic"})
@RequiredArgsConstructor
public class PermissionQueryServiceImpl implements PermissionQueryService {
    
    private final PermissionMappingService mappingService;

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<RoleDTO> getAllRoleDtos() {
        List<Role> roles = roleRepository.findAll();

        List<RoleDTO> dtos = roles.stream().map(role -> mappingService.toDTO(role)).toList();;
        return dtos;
    }

    public List<PermissionDTO> getAllPermissionDtos() {
        List<Permission> perms = permissionRepository.findAll();

        List<PermissionDTO> dtos = perms.stream().map(perm -> mappingService.toDTO(perm)).toList();
        return dtos;
    }

    public List<PermissionDTO> getPermissionDtosByRole(@NonNull Long roleId) {
        Role role = roleRepository.findById(roleId).orElse(null);
        Set<Permission> roles = role.getPermissions();
        
        List<PermissionDTO> dtos = roles.stream().map(perm -> mappingService.toDTO(perm)).toList();
        return dtos;
    }
}

