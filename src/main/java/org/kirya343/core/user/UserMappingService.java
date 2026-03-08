package org.kirya343.core.user;

import java.util.Collection;
import java.util.List;

import org.kirya343.datasource.model.user.User;
import org.kirya343.dto.user.FullUserDTO;
import org.kirya343.dto.user.ShortUserDTO;
import org.kirya343.dto.user.UserDTO;

public interface UserMappingService {

    UserDTO toDTO(User user);
    ShortUserDTO toShortDTO(User user);
    FullUserDTO toFullDto(User user);
    List<UserDTO> toDTOList(Collection<User> users);
    List<ShortUserDTO> toShortDTOList(Collection<User> users);
}