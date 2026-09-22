package org.kirya343.infrastructure.security.services;

import java.util.Objects;

import org.kirya343.features.user.datasource.User;
import org.kirya343.features.user.datasource.UserRepository;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAuthDataService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserAuthData load(@NonNull Long userId) {

        User user = userRepository.findById(userId).orElseThrow(
            () -> new EntityNotFoundException("Пользователь не найден"));

        return new UserAuthData(
            Objects.requireNonNull(user.getId()),
            Objects.requireNonNull(user.getOpenId()),
            user.getName(),
            Objects.requireNonNull(user.getStatus())
        );
    }

    @Transactional(readOnly = true)
    public UserAuthData load(@NonNull String apiKey) {

        User user = userRepository.findByApiKey(apiKey).orElseThrow(
            () -> new EntityNotFoundException("Пользователь не найден"));

        return new UserAuthData(
            Objects.requireNonNull(user.getId()),
            Objects.requireNonNull(user.getOpenId()),
            user.getName(),
            Objects.requireNonNull(user.getStatus())
        );
    }
}