package org.kirya343.features.playback.services;

import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.infrastructure.diagnostics.models.TextResult;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class PlaybackDiagnosticsService {

    private final RoomPlaybackContextStore roomPlaybackContextStore;

    public TextResult inspectRoomStreamer(Long roomId) {

        RoomPlaybackContext context = roomPlaybackContextStore.get(roomId);

        if (context == null) {
            return new TextResult("""
                Room: %s
                Status: %s
            """.formatted(roomId, "OFFLINE"));
        }

        return new TextResult("""
            Room: %s
            Status: %s
            Listeners: %s
        """.formatted(roomId, "ONLINE", context.getListeners()));
    };
}