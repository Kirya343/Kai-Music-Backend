package org.kirya343.features.presence;

import org.kirya343.features.presence.services.PresenceService;
import org.kirya343.features.user.dto.event.UserConnectedEvent;
import org.kirya343.features.user.dto.event.UserDisconnectedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioUserConnectionEventHandler {

    private final PresenceService presenceService;

    @EventListener
    public void handleConnected(UserConnectedEvent event) {
        
        presenceService.userConnected(event.authData().openId());
    }

    @EventListener
    public void handleDisconnected(UserDisconnectedEvent event) {
        
        presenceService.userDisconnected(event.authData().openId());
    }
}
