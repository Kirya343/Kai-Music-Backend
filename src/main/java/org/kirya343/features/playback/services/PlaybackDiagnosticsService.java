package org.kirya343.features.playback.services;

import java.util.Map;

import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.infrastructure.diagnostics.models.DiagnosticResult;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class PlaybackDiagnosticsService {

    private final RoomPlaybackContextStore roomPlaybackContextStore;

    public DiagnosticResult inspectRoomStreamer(Long roomId) {

        RoomPlaybackContext context = roomPlaybackContextStore.get(roomId);

        if (context == null) {
            return new DiagnosticResult(
                roomId.toString(), 
                Map.of("Status", "OFFLINE")
            );
        }

        return new DiagnosticResult(
            roomId.toString(), 
            Map.of(
                "Status", "ONLINE",
                "Listeners", context.getListeners()
            )
        );
    };
}