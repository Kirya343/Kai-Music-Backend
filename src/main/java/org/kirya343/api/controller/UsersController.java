package org.kirya343.api.controller;

import org.kirya343.core.user.UserQueryService;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.user.UserDTO;
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