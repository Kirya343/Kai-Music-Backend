package org.kirya343.core.user.impl;

import java.util.List;

import org.kirya343.core.user.UserMappingService;
import org.kirya343.core.user.UserQueryService;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.repository.user.UserRepository;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.user.UserDTO;
import org.kirya343.enums.UserStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;
    private final UserMappingService userMappingService;
    
    public List<User> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public UserDTO getCurrentUser(UserAuthData authData) {
        User user = userRepository.getFullUser(authData.id()).orElseThrow(
            () -> new EntityNotFoundException("Пользователь не найден"));
        return userMappingService.toDTO(user);
    }

    public List<UserDTO> getRecentUsers(int count) {
        List<User> users = userRepository.findAllByStatusOrderByCreatedAtDesc(PageRequest.of(0, count), UserStatus.ACTIVE).getContent();

        return userMappingService.toDTOList(users);
    }
}