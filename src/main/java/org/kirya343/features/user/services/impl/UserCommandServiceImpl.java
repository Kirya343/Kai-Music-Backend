package org.kirya343.features.user.services.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.kirya343.features.user.services.UserCommandService;
import org.kirya343.features.user.datasource.User;
import org.kirya343.features.permission.datasource.model.Role;
import org.kirya343.features.user.datasource.UserRepository;
import org.kirya343.features.user.enums.AuthProvider;
import org.kirya343.features.user.enums.UserStatus;
import org.kirya343.features.permission.datasource.repository.RoleRepository;
import org.kirya343.features.authentication.dto.RegisterRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile({"production", "statistic"})
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public void modifyUserParam(User user, Map<String, Object> updates) {

        if (user != null) {
            updates.forEach((key, value) -> {
                switch (key) {
                    case "name":
                        user.setName((String) value);
                        break;
                    case "avatarUrl":
                        user.setAvatarUrl((String) value);
                        break;
                }
            });

            userRepository.save(user);
        }
    }

    public User registerOauthUser(User user) {
        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }

    public User registerLocal(RegisterRequest regRequest, HttpServletRequest request) {
        
        String hashed = passwordEncoder.encode(regRequest.password());
        Role role = roleRepository.findByName("USER");
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        User user = new User(
            regRequest.email(),
            regRequest.name(), 
            hashed, 
            roles
        );

        return userRepository.save(user);
    }

    public boolean authenticate(User user, String rawPassword) {
        if (user == null) return false;
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public Map<String, String> updateUserPassword(User user, String oldPassword, String newPassword) {
        String oldHashed = passwordEncoder.encode(oldPassword);
        String newHashed = passwordEncoder.encode(newPassword);
        if (user.getPasswordHash() == null && !user.getProviders().contains(AuthProvider.LOCAL)) {
            user.getProviders().add(AuthProvider.LOCAL);
            user.setPasswordHash(newHashed);
            userRepository.save(user);
            return Map.of("message", "Теперь в ваш аккаунт можно зайти по паролю, для логина используйте почту от вашего дискорд аккаунта", "type", "success");
        }

        if (!user.getPasswordHash().equals(oldHashed)) {
            return Map.of("message", "Неверный пароль", "type", "error");
        }

        user.setPasswordHash(newHashed);
        userRepository.save(user);

        return Map.of("message", "Пароль от аккаунта обновлён", "type", "success");
    }
}
