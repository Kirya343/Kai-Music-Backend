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
public class ListRoomStreamersCommand implements DiagnosticCommand {

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
        return new StreamingResult(output -> {
            while (!Thread.currentThread().isInterrupted()) {
                String content = roomPlaybackContextStore.getAll()
                    .entrySet()
                    .stream()
                    .map(entry -> {

                        boolean isOnline = roomStreamWorkerManager.checkWorker(entry.getKey());

                        RoomPlaybackContext context = roomPlaybackContextStore.get(entry.getKey());

                        AudioFile audio = context.getCurrentAudio();

                        if (isOnline) {

                            return String.join("\n",
                                "Room: " + entry.getKey(),
                                "    Status: Online",
                                "    Listeners: " + context.getListeners(),
                                "    Playing Audio: "
                                    + (audio != null ? audio.getName() : null)
                            );
                        } else {
                            return String.join("\n",
                                "Room: " + entry.getKey(),
                                "    Status: OFFLINE"
                            );
                        }
                    })
                    .collect(Collectors.joining("\n"));

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