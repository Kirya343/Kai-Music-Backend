package org.kirya343.features.user.services;

import java.util.List;

import org.kirya343.features.user.datasource.User;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.user.dto.UserDTO;

public interface UserQueryService {

    List<User> findAll();
    List<UserDTO> getRecentUsers(int count);

    UserDTO getCurrentUser(UserAuthData authData);
}

