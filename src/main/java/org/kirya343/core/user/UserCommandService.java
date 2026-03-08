package org.kirya343.core.user;

import java.util.Map;

import org.kirya343.datasource.model.user.User;
import org.kirya343.dto.auth.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;

public interface UserCommandService {

    boolean authenticate(User user, String rawPassword);
    User registerLocal(RegisterRequest regRequest, HttpServletRequest request);

    void modifyUserParam(User user, Map<String, Object> updates);
    Map<String, String> updateUserPassword(User user, String oldPassword, String newPassword);
}
