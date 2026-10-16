package org.kirya343.features.playback.diagnostics;

import java.util.stream.Collectors;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playback.services.streaming.RoomStreamWorkerManager;
import org.kirya343.infrastructure.diagnostics.commands.DiagnosticCommand;
import org.kirya343.infrastructure.diagnostics.models.StreamMode;
import org.kirya343.infrastructure.diagnostics.models.StreamingResult;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class InspectRoomStreamerCommand implements DiagnosticCommand {

    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final RoomStreamWorkerManager roomStreamWorkerManager;

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

        return new StreamingResult(output -> {
            while (!Thread.currentThread().isInterrupted()) {

                boolean isOnline = roomStreamWorkerManager.checkWorker(roomId);

                String content = "";

                if (isOnline) {
                    String workerContent = roomStreamWorkerManager.getWorker(roomId)
                        .getDiagnosticsContent();

                    RoomPlaybackContext context = roomPlaybackContextStore.get(roomId);

                    AudioFile audio = context.getCurrentAudio();

                    content = String.join("\n",
                        "Room: " + roomId,
                        "    Status: ONLINE",
                        "    Listeners: " + context.getListeners(),
                        "",
                        "    Playing Audio: ",
                        "       Id: " + audio.getId(),
                        "       Name: " + audio.getName(),
                        "       Title: " + audio.getTitle(),
                        "       Path: " + audio.getPath(),
                        "       Duration: " + audio.getDuration() + "s",
                        "       Chunks: " + audio.getChunks(),
                        "",
                        "    Stream worker:",
                        workerContent.lines()
                            .map(line -> "        " + line.stripLeading())
                            .collect(Collectors.joining("\n"))
                    );
                } else {
                    content = String.join("\n",
                        "Room: " + roomId,
                        "    Status: OFFLINE"
                    );
                }

                output.accept(content);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }, StreamMode.REPLACE);
    }
}