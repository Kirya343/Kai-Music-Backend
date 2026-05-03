package org.kirya343.dto.user;

import java.time.LocalDateTime;
import java.util.List;

import org.kirya343.enums.UserStatus;

public record FullUserDTO(
    String openId,
    String name,
    String avatarUrl,
    UserStatus status,

    List<RoleDTO> roles,
    LocalDateTime createdAt
) {}