package org.kirya343.core.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

@RequiredArgsConstructor
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private final String cookieName;

    @Override
    public String resolve(HttpServletRequest request) {
        if (request.getHeader("Authorization") != null) {
            return null; // позволяем заголовку иметь приоритет
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (cookieName.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
