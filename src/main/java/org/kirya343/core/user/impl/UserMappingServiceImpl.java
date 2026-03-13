package org.kirya343.core.user.impl;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

import org.kirya343.core.user.UserMappingService;
import org.kirya343.core.user.permission.PermissionMappingService;
import org.kirya343.datasource.model.user.User;
import org.kirya343.dto.user.FullUserDTO;
import org.kirya343.dto.user.RoleDTO;
import org.kirya343.dto.user.ShortUserDTO;
import org.kirya343.dto.user.UserDTO;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMappingServiceImpl implements UserMappingService {

    private final PermissionMappingService permissionMappingService;

    public UserDTO toDTO(User user) {

        if (user == null) return null;
        
        List<RoleDTO> roles = user.getRoles().stream().map(role -> permissionMappingService.toDTO(role)).toList();
                              
        UserDTO dto = new UserDTO(
            user.getId(), 
            user.getOpenId(), 
            user.getName(), 
            user.getAvatarUrl(),
            roles,
            user.getEmail(),
            user.getStatus().toString(),
            LocalDateTime.ofInstant(user.getCreatedAt(), ZoneId.systemDefault())
        );
        return dto;
    }

    public ShortUserDTO toShortDTO(User user) {
        return new ShortUserDTO(user.getId(), user.getOpenId(), user.getName(), user.getAvatarUrl());
    }

    public List<UserDTO> toDTOList(Collection<User> users) {
        return users.stream().map(user -> toDTO(user)).toList();
    }

    public List<ShortUserDTO> toShortDTOList(Collection<User> users) {
        return users.stream().map(user -> toShortDTO(user)).toList();
    }

    public FullUserDTO toFullDto(User user) {

        if (user == null) return null;

        List<RoleDTO> roles = user.getRoles().stream().map(role -> permissionMappingService.toDTO(role)).toList();

        FullUserDTO dto = new FullUserDTO(
            user.getId(),
            user.getOpenId(),
            user.getName(),
            user.getAvatarUrl(),
            user.getStatus(),
            roles,
            LocalDateTime.ofInstant(user.getCreatedAt(), ZoneId.systemDefault())
        );

        return dto;
    }
}


