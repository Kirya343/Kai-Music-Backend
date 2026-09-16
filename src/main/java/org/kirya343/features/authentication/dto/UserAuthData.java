package org.kirya343.features.authentication.dto;

import java.util.Objects;

import org.kirya343.features.user.enums.UserStatus;
import org.springframework.lang.NonNull;

public record UserAuthData(
    @NonNull Long id,
    @NonNull String openId,
    String name,
    @NonNull UserStatus status
) {
    public UserAuthData {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(openId, "openId must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}