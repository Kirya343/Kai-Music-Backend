package org.kirya343.core.presence;

import org.kirya343.core.presence.event.PresenceChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Component
@Slf4j
public class PresenceChangedListener {

    private final PresenceService presenceService;

    @EventListener
    public void onPresenceChanged(PresenceChangedEvent event) {

        switch (event.presence()) {
            case ONLINE -> presenceService.userConnected(event.userOpenId());
            case OFFLINE -> presenceService.userDisconnected(event.userOpenId());
            default -> throw new IllegalStateException();
        }
    }
}
