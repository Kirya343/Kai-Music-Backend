package org.kirya343.core.security.services;

import java.util.Collection;
import java.util.Objects;

import org.kirya343.core.security.UserJwtAuthenticationToken;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.enums.UserStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CachedPermissionsJwtTokenConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final PermissionsService permissionsService;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {

        Long userId = Objects.requireNonNull(Long.valueOf(jwt.getSubject()));

        Collection<GrantedAuthority> authorities =
            permissionsService.getUserPermissions(userId);

        UserAuthData authData = new UserAuthData(
            Objects.requireNonNull(userId),
            Objects.requireNonNull(jwt.getClaim("openId")),
            jwt.getClaim("name"),
            UserStatus.valueOf(jwt.getClaim("status"))
        );

        return new UserJwtAuthenticationToken(jwt, authorities, authData);
    }
}
