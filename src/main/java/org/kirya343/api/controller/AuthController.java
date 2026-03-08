package org.kirya343.api.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.kirya343.core.security.AuthCookiesService;
import org.kirya343.core.security.JwtService;
import org.kirya343.core.security.services.AuthService;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.repository.user.UserRepository;
import org.kirya343.dto.auth.LoginRequest;
import org.kirya343.dto.auth.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthCookiesService cookiesService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthService authService;
    
    @Value("${api.url}")
    private String apiUrl;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @Value("${app.cookie.sameSite}")
    private String cookieSameSite;

    @PostMapping("/login")
    @PermitAll
    public ResponseEntity<?> login(
        @Valid @RequestBody LoginRequest request, 
        HttpServletResponse response
    ) throws IOException, ServletException {
        return authService.login(request, response);
    }

    @PostMapping("/register")
    @PermitAll
    public ResponseEntity<?> register(
        @Valid @RequestBody RegisterRequest regRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return authService.registerLocal(regRequest, request, response);
    }

    @GetMapping("/discord")
    @PermitAll
    public void redirectToDiscord(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {

        response.sendRedirect("/login/oauth2/code/discord");
    }

    @PostMapping("/refresh")
    @PermitAll
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("Обновляем токен пользователя");
        try {
            String refreshToken = getTokenFromCookies(request, "refreshToken");
            logger.debug("Токен найден? {}", refreshToken);

            Long userId = null;
            if (refreshToken != null) {
                userId = Long.valueOf(jwtService.validateAndGetUserIdStr(refreshToken));
            }

            logger.debug("userId: {}", userId);

            User user = null;

            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
                logger.debug("Пользователь найден");
            }

            logger.debug("Айди пользователя: {}", user != null ? user.getId() : 0);

            if (user == null) throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Пользователя не сущестует");

            cookiesService.setAuthCookies(response, user);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Token generation error: " + e.getMessage());
        }
    }

    private String getTokenFromCookies(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    @PostMapping("/logout")
    @PermitAll
    public ResponseEntity<?> logout(HttpServletResponse response) {

        try {
            
            cookiesService.deleteAuthCookies(response);

            return ResponseEntity.ok(Map.of("message", "Вы успешно вышли из аккаунта"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Token generation error: " + e.getMessage());
        }
    }
}
