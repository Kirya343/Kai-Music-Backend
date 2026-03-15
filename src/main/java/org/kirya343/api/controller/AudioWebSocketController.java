package org.kirya343.api.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.core.audio.room.RoomManager;
import org.kirya343.dto.audio.PlaybackStateDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AudioWebSocketController {

    private final RoomManager roomManager;
    private final Map<String, Long> lastUpdate = new ConcurrentHashMap<>();
    private static final long UPDATE_DELAY_MS = 300;

    private boolean shouldIgnore(Long userId, Long roomId) {
        long now = System.currentTimeMillis();

        String key = userId + ":" + roomId;

        Long last = lastUpdate.get(key);

        if (last != null && now - last < UPDATE_DELAY_MS) {
            return true;
        }

        lastUpdate.put(key, now);
        return false;
    }
    
    @MessageMapping("/room/{roomId}/update-playback-state")
    public void updatePlaybackState(
        PlaybackStateDTO state,
        @DestinationVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        if (shouldIgnore(authData.id(), roomId)) {
            return;
        }

        if (!state.pause()) {
            roomManager.playTrack(roomId, state);
        } else {
            roomManager.pause(roomId, state);
        }
    }
}
