package org.kirya343.dto.user;

import java.time.LocalDateTime;
import java.util.List;

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
