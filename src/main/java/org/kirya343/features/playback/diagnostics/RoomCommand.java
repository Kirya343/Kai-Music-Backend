package org.kirya343.features.playback.diagnostics;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
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
public class RoomCommand implements DiagnosticCommand {

    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final RoomStreamWorkerManager roomStreamWorkerManager;
    private final PlaybackDiagnosticService diagnosticService;

    @Override
    public String getName() {
        return "room";
    }

    @Override
    public String getDescription() {
        return "Inspects a room streamer by room ID";
    }

    @Override
    public StreamingResult execute(String[] args) {

        if (args.length != 1) {
            throw new IllegalArgumentException(
                "Usage: room <roomId>"
            );
        }

        long roomId;

        try {
            roomId = Long.parseLong(args[0]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "Room ID must be a number: " + args[0]
            );
        }

        return new StreamingResult(
            () -> getRoomStreamerData(roomId), 
            StreamMode.REPLACE
        );
    }

    private String getRoomStreamerData(Long roomId) {
        boolean isOnline = roomStreamWorkerManager.checkWorker(roomId);

        DiagnosticTextBuilder builder = null;

        if (isOnline) {

            RoomPlaybackContext context = roomPlaybackContextStore.get(roomId);

            AudioFile audio = context.getCurrentAudio();

            builder = diagnosticService.renderOnlineRoom(roomId);

            builder.addSection(diagnosticService.renderAudioData(audio));
            builder.addSection(diagnosticService.renderStreamWorker(roomId));
        } else {
            builder = diagnosticService.renderOfflineRoom(roomId);
        }

        return builder.getContent();
    }
}