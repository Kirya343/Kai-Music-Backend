package org.kirya343.features.playback.diagnostics;

import org.kirya343.features.playback.services.PlaybackDiagnosticsService;
import org.kirya343.infrastructure.diagnostics.commands.DiagnosticCommand;
import org.kirya343.infrastructure.diagnostics.models.TextResult;
import org.springframework.stereotype.Component;

@Component
public class InspectRoomStreamerCommand implements DiagnosticCommand {

    private final PlaybackDiagnosticsService diagnosticsService;

    public InspectRoomStreamerCommand(
        PlaybackDiagnosticsService diagnosticsService
    ) {
        this.diagnosticsService = diagnosticsService;
    }

    @Override
    public String getName() {
        return "playback.room";
    }

    @Override
    public String getDescription() {
        return "Inspects a room streamer by room ID";
    }

    @Override
    public TextResult execute(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                "Usage: playback.room <roomId>"
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

        return diagnosticsService.inspectRoomStreamer(roomId);
    }
}