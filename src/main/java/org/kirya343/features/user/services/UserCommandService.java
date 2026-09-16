package org.kirya343.features.user.services;

import java.util.Map;

import org.kirya343.features.user.datasource.User;
import org.kirya343.features.authentication.dto.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;

public interface UserCommandService {

    boolean authenticate(User user, String rawPassword);
    User registerLocal(RegisterRequest regRequest, HttpServletRequest request);

    void modifyUserParam(User user, Map<String, Object> updates);
    Map<String, String> updateUserPassword(User user, String oldPassword, String newPassword);
}
