package org.kirya343.features.permission.dto;

import java.util.Collection;
import java.util.List;

import org.kirya343.features.permission.datasource.model.Role;

public record RoleDTO(
    Long id,
    String name,
    int level
) {
    public static RoleDTO ofRole(Role role) {
        return new RoleDTO(role.getId(), role.getName(), role.getLevel());
    }

    public static List<RoleDTO> ofList(Collection<Role> roles) {
        return roles.stream().map(r -> RoleDTO.ofRole(r)).toList();
    }
}
