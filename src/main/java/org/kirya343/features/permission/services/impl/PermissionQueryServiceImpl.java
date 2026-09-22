package org.kirya343.features.permission.services.impl;

import java.util.List;
import java.util.Set;

import org.kirya343.features.permission.services.PermissionMappingService;
import org.kirya343.features.permission.services.PermissionQueryService;
import org.kirya343.features.permission.datasource.model.Permission;
import org.kirya343.features.permission.datasource.model.Role;
import org.kirya343.features.permission.datasource.repository.PermissionRepository;
import org.kirya343.features.permission.datasource.repository.RoleRepository;
import org.kirya343.features.permission.dto.PermissionDTO;
import org.kirya343.features.permission.dto.RoleDTO;
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

