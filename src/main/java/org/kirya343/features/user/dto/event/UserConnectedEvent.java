package org.kirya343.features.user.dto.event;

import org.kirya343.features.authentication.dto.UserAuthData;

public record UserConnectedEvent(
    UserAuthData authData
) {
}
