package org.kirya343.features.playback.diagnostics;

import java.util.Set;
import java.util.stream.Collectors;

import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playback.services.streaming.RoomStreamWorkerManager;
import org.kirya343.infrastructure.diagnostics.commands.DiagnosticCommand;
import org.kirya343.infrastructure.diagnostics.core.DiagnosticTextBuilder;
import org.kirya343.infrastructure.diagnostics.models.StreamMode;
import org.kirya343.infrastructure.diagnostics.models.StreamingResult;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class RoomsCommand implements DiagnosticCommand {

    private final PlaybackDiagnosticService diagnosticService;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final RoomStreamWorkerManager roomStreamWorkerManager;

    @Override
    public String getName() {
        return "rooms";
    }

    @Override
    public String getDescription() {
        return "Lists active room streamers";
    }

    @Override
    public StreamingResult execute(String[] args) {
        return new StreamingResult(
            this::getRoomsData, 
            StreamMode.REPLACE
        );
    }

    private String getRoomsData() {

        Set<Long> keys = roomPlaybackContextStore.getAll().keySet();

        if (keys.size() == 0) return "No rooms online";

        return keys.stream()
            .map(key -> renderRoomData(key))
            .collect(Collectors.joining("\n"));
    }

    private String renderRoomData(Long roomId) {
        boolean isOnline = roomStreamWorkerManager.checkWorker(roomId);

        DiagnosticTextBuilder builder = null;

        if (isOnline) {
            builder = diagnosticService.renderOnlineRoom(roomId);
        } else {
            builder = diagnosticService.renderOfflineRoom(roomId);
        }

        return builder.getContent();
    }
}