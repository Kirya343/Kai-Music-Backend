package org.kirya343.core.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.enums.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenConverter.class);

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        String userIdStr = jwt.getSubject();
        logger.debug("userId: {}", userIdStr);
        Long userId = Long.valueOf(userIdStr);

        String userOpenId = Objects.requireNonNull(jwt.getClaim("openId"));
        UserStatus userStatus = UserStatus.valueOf(Objects.requireNonNull(jwt.getClaim("status")));
        String userName = jwt.getClaim("name");

        if (userId == null) {
            throw new IllegalStateException("ID пользователя не найден. AccessToken не действителен");
        }

        UserAuthData authData = new UserAuthData(userId, userOpenId, userName, userStatus);

        return new UserJwtAuthenticationToken(jwt, authorities, authData);
    }
}
