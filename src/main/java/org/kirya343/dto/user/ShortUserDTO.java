package org.kirya343.dto.user;

public record ShortUserDTO(
    Long id,
    String openId,
    String name,
    String avatarUrl
) {
}
