package org.kirya343.features.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.kirya343.features.user.enums.UserStatus;
import org.kirya343.features.permission.dto.RoleDTO;

public record FullUserDTO(
    String openId,
    String name,
    String avatarUrl,
    UserStatus status,

    List<RoleDTO> roles,
    LocalDateTime createdAt
) {}