package org.kirya343.features.playback.controller;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.user.dto.event.UserConnectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller 
@Slf4j 
@RequiredArgsConstructor 
public class PlaybackWebSocketController {

    private final ApplicationEventPublisher eventPublisher;

    @MessageMapping("/user.ready")
    public void updatePlaybackState(
        @AuthenticationPrincipal UserAuthData authData
    ) {

        eventPublisher.publishEvent(new UserConnectedEvent(authData));
    }
}
