package org.kirya343.features.authentication.service;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.user.datasource.User;
import org.kirya343.features.user.datasource.UserRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class UserAuthDataService {

    private final UserRepository userRepository;
    
    public User getByAuthData(UserAuthData authData) {
        return userRepository.findById(authData.id()).orElse(null);
    }
}
