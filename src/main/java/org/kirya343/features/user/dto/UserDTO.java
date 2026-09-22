package org.kirya343.features.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.kirya343.features.permission.dto.RoleDTO;

public record UserDTO(
    Long id,
    String openId,
    String name,
    String avatarUrl,
    List<RoleDTO> roles,
    String email,
    String status,
    LocalDateTime createdAt
) {
}
