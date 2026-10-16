package org.kirya343.features.playback.diagnostics;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playback.services.streaming.RoomStreamWorkerManager;
import org.kirya343.infrastructure.diagnostics.core.DiagnosticTextBuilder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class PlaybackDiagnosticService {

    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final RoomStreamWorkerManager roomStreamWorkerManager;
    
    public DiagnosticTextBuilder renderOfflineRoom(Long roomId) {
        return new DiagnosticTextBuilder()
            .setTitle("Room: " + roomId)
            .addRow("Status: OFFLINE");
    }

    public DiagnosticTextBuilder renderOnlineRoom(Long roomId) {

        RoomPlaybackContext context = roomPlaybackContextStore.get(roomId);

        AudioFile audio = context.getCurrentAudio();

        return new DiagnosticTextBuilder()
            .setTitle("Room: " + roomId)
            .addRow("Status: ONLINE")
            .addRow("Listeners: " + context.getListeners())
            .addRow( "Playing Audio: " + (audio != null ? audio.getName() : null));
    }

    public DiagnosticTextBuilder renderAudioData(AudioFile audio) {
        return new DiagnosticTextBuilder()
            .setTitle("Playing Audio:")
            .addRow("Id: " + audio.getId())
            .addRow("Name: " + audio.getName())
            .addRow("Title: " + audio.getTitle())
            .addRow("Path: " + audio.getPath())
            .addRow("Duration: " +  audio.getDuration() + "s")
            .addRow("Chunks: " + audio.getChunks());
    }

    public DiagnosticTextBuilder renderStreamWorker(Long roomId) {

        DiagnosticTextBuilder builder = roomStreamWorkerManager.getWorker(roomId)
            .getData();

        builder.setTitle("Stream worker:");

        return builder;
    }
}
