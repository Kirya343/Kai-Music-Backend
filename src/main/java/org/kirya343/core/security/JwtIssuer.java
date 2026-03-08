package org.kirya343.core.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.model.user.permission.Permission;
import org.kirya343.datasource.model.user.permission.Role;
import org.kirya343.datasource.repository.user.UserRepository;
import org.kirya343.enums.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtIssuer {
    
    private final RSAKey rsaKey;
    private final UserRepository userRepository;

    @Value("${isTest}")
    private boolean isTest;

    public String issueAccessToken(User user) throws JOSEException {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        if (isTest && !userRepository.existsByIdAndRoles_Name(user.getId(), "ADMIN")) {
            throw new AccessDeniedException("Нет доступа к сервису, он закрыт на технические работы");
        }

        if (user.getStatus().equals(UserStatus.BLOCKED)) {
            throw new AccessDeniedException("Ваш аккаунт заблокирован");
        }

        Instant now = Instant.now();
        JWSSigner signer = new RSASSASigner(rsaKey);

        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        for (Role role : user.getRoles()) {
            roles.add("ROLE_" + role.getName().toUpperCase());
            for (Permission perm : role.getPermissions()) {
                permissions.add(perm.getName().toUpperCase());
            }
        }

        // Строим JWT
        JWTClaimsSet set = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(30)))) // TTL 30 мин (для теста 3 минуты, вернуть на 30) 
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("openId", user.getOpenId())
                .claim("name", user.getName())
                .claim("status", user.getStatus())
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT jwt = new SignedJWT(header, set);
        jwt.sign(signer);
        return jwt.serialize();
    }

    public String issueRefreshToken(User user) throws JOSEException {

        Instant now = Instant.now();
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet set = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofDays(30))))
                .claim("openId", user.getOpenId())
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT jwt = new SignedJWT(header, set);
        jwt.sign(signer);

        return jwt.serialize();
    }
}
