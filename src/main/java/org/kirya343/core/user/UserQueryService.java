package org.kirya343.core.user;

import java.util.List;

import org.kirya343.datasource.model.user.User;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.user.UserDTO;

public interface UserQueryService {

    List<User> findAll();
    List<UserDTO> getRecentUsers(int count);

    UserDTO getCurrentUser(UserAuthData authData);
}

