package org.kirya343.api.controller;

import org.kirya343.core.user.UserQueryService;
import org.kirya343.datasource.repository.user.UserRepository;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.user.UserDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UsersController {

    private final UserQueryService userQueryService;
    private final UserRepository userRepository;

    @GetMapping("/current")
    @PermitAll //@PreAuthorize("hasAuthority('GET_CURRENT_USER')")
    public UserDTO getCurrentUser(@AuthenticationPrincipal UserAuthData authData) {
        return userQueryService.getCurrentUser(authData);
    }

    @PatchMapping("/room")
    public void setUserRoom(
        @RequestParam Long roomId,
        @AuthenticationPrincipal UserAuthData authData) {
        userRepository.updateListeningRoom(authData.id(), roomId);
    }
}