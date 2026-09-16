package org.kirya343.features.user.controller;

import org.kirya343.features.user.services.UserQueryService;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.user.dto.UserDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UsersController {

    private final UserQueryService userQueryService;

    @GetMapping("/current")
    @PermitAll //@PreAuthorize("hasAuthority('GET_CURRENT_USER')")
    public UserDTO getCurrentUser(@AuthenticationPrincipal UserAuthData authData) {
        return userQueryService.getCurrentUser(authData);
    }
}