package org.kirya343.core.security.services;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.kirya343.datasource.model.user.permission.Permission;
import org.kirya343.datasource.model.user.permission.Role;
import org.kirya343.datasource.repository.user.permission.RoleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionsService {

    private final RoleRepository roleRepository;

    @Cacheable(
        value = "user-permissions",
        key = "#userId"
    )
    public Collection<GrantedAuthority> getUserPermissions(@NonNull Long userId) {

        Set<Role> roles = roleRepository.findRolesWithPermissionsByUserId(userId);

        Set<GrantedAuthority> authorities = new HashSet<>();

        // permissions
        roles.stream()
            .filter(role -> role.getPermissions() != null)
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getName)
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);

        // roles with ROLE_
        roles.stream()
            .map(Role::getName)
            .map(roleName -> "ROLE_" + roleName)
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);

        return authorities;
    }
}