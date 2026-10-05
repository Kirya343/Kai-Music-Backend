package org.kirya343.features.permission.services;

import java.util.List;
import java.util.Set;

import org.kirya343.features.permission.services.PermissionQueryService;
import org.kirya343.features.permission.datasource.model.Permission;
import org.kirya343.features.permission.datasource.model.Role;
import org.kirya343.features.permission.datasource.repository.PermissionRepository;
import org.kirya343.features.permission.datasource.repository.RoleRepository;
import org.kirya343.features.permission.dto.PermissionDTO;
import org.kirya343.features.permission.dto.RoleDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@Profile({"production", "statistic"})
@RequiredArgsConstructor
public class PermissionQueryService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<RoleDTO> getAllRoleDtos() {
        List<Role> roles = roleRepository.findAll();
        return RoleDTO.ofList(roles);
    }

    public List<PermissionDTO> getAllPermissionDtos() {
        List<Permission> perms = permissionRepository.findAll();
        return PermissionDTO.ofList(perms);
    }

    public List<PermissionDTO> getPermissionDtosByRole(Long roleId) {
        Role role = roleRepository.findById(roleId).orElseThrow();
        Set<Permission> perms = role.getPermissions();

        return PermissionDTO.ofList(perms);
    }
}

