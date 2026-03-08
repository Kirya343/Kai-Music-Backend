package org.kirya343.core.security.services;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.kirya343.core.exceptions.UserNotFoundException;
import org.kirya343.core.security.AuthCookiesService;
import org.kirya343.core.security.JwtService;
import org.kirya343.core.user.UserCommandService;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.repository.user.UserRepository;
import org.kirya343.dto.auth.LoginRequest;
import org.kirya343.dto.auth.RegisterRequest;
import org.kirya343.enums.AuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserCommandService userCommandService;
    private final AuthCookiesService cookiesService;
    
    public ResponseEntity<?> login(LoginRequest request, HttpServletResponse response) {

        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Аккаунт не существует"));
        }

        if (!user.getProviders().contains(AuthProvider.LOCAL)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "В этот аккаунт нельзя зайти через пароль"));
        }

        if (!userCommandService.authenticate(user, request.password())) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Неверный пароль"));
        }

        try {
            cookiesService.setAuthCookies(response, user);
        } catch (ServletException e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Вы успешно авторизовались"));
    }

    public ResponseEntity<?> registerLocal(
        RegisterRequest regRequest, 
        HttpServletRequest request,
        HttpServletResponse response
    ) {

        if (userRepository.existsByName(regRequest.name())) {
            logger.debug("Пользователь с таким именем уже зарегистрирован");
            return ResponseEntity.ok(
                Map.of("success", false, 
                       "message", "Это имя уже используется"
                ));
        }

        User user;
        
        try {
            user = userCommandService.registerLocal(regRequest, request);
        } catch (DataIntegrityViolationException e) {
            logger.warn("User register constraint violated: {}", e.getMessage());
            return ResponseEntity.ok(
                Map.of("success", false, "message", "Никнейм или почта уже зарегистрированы")
            );
        }
        logger.debug("User registered");

        try {
            cookiesService.setAuthCookies(response, user);
        } catch (ServletException e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "register_success"));
    }

    public void refreshToken( HttpServletRequest request, HttpServletResponse response) {
        logger.debug("Обновляем токен пользователя");
        try {

            Long userId = jwtService.validateAndGetUserId(getTokenFromCookies(request, "refreshToken"));

            logger.debug("userId: {}", userId);

            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
                logger.debug("Пользователь найден по id: {}", user != null);

                if (user == null) {
                    logger.debug("Пользователь не найден, создаём временного");
                    throw new UserNotFoundException(userId.toString());
                }
            } else {
                throw new IllegalStateException("RefreshToken не содержит ID пользователя. Авторизуйтесь повторно");
            }

            userRepository.save(user);

            cookiesService.setAuthCookies(response, user); // 4. обновляем куки с токенами

        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка сервера при обновлении токена.");
        }
    }

    public void redirectToGoogle(
        String redirect,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        logger.debug("redirect: " + redirect);

        Long userId = jwtService.validateAndGetUserId(getTokenFromCookies(request, "refreshToken"));
        String redirectUrl = URLEncoder.encode(redirect, StandardCharsets.UTF_8);

        request.getSession().setAttribute("tempUserId", userId);
        request.getSession().setAttribute("redirectUrl", redirectUrl);

        response.sendRedirect("/oauth2/authorization/google");
    }

    private String getTokenFromCookies(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
