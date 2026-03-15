package org.kirya343.core.security.websocket;

import java.security.Principal;
import org.kirya343.dto.auth.UserAuthData;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthDataPrincipal implements Principal {

    private final UserAuthData authData;

    @Override
    public String getName() {
        // имя пользователя для Spring /user/queue/…
        return authData.openId();  
    }

    public UserAuthData getAuthData() {
        return authData;
    }
}
