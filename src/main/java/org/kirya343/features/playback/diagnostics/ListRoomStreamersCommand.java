package org.kirya343.features.playback.diagnostics;

import java.util.stream.Collectors;

import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.infrastructure.diagnostics.commands.DiagnosticCommand;
import org.kirya343.infrastructure.diagnostics.models.StreamMode;
import org.kirya343.infrastructure.diagnostics.models.StreamingResult;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class ListRoomStreamersCommand implements DiagnosticCommand {

    private final RoomPlaybackContextStore roomPlaybackContextStore;

    @Override
    public String getName() {
        return "playback.rooms";
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
                    .map(entry -> entry.getKey() + " = " + entry.getValue())
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