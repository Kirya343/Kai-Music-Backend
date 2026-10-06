package org.kirya343.features.user.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.kirya343.features.user.datasource.User;
import org.kirya343.features.user.enums.UserStatus;
import org.kirya343.features.permission.dto.RoleDTO;

public record FullUserDTO(
    String openId,
    String name,
    String avatarUrl,
    UserStatus status,

    List<RoleDTO> roles,
    LocalDateTime createdAt
) {
    public static FullUserDTO ofUser(User user) {

        if (user == null) return null;

        return new FullUserDTO(
            user.getOpenId(),
            user.getName(),
            user.getAvatarUrl(),
            user.getStatus(),
            RoleDTO.ofList(user.getRoles()),
            LocalDateTime.ofInstant(user.getCreatedAt(), ZoneId.systemDefault())
        );
    }
}