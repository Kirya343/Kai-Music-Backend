package org.kirya343.features.user.services;

import java.util.Collection;
import java.util.List;

import org.kirya343.features.user.datasource.User;
import org.kirya343.features.user.dto.FullUserDTO;
import org.kirya343.features.user.dto.ShortUserDTO;
import org.kirya343.features.user.dto.UserDTO;

public interface UserMappingService {

    UserDTO toDTO(User user);
    ShortUserDTO toShortDTO(User user);
    FullUserDTO toFullDto(User user);
    List<UserDTO> toDTOList(Collection<User> users);
    List<ShortUserDTO> toShortDTOList(Collection<User> users);
}