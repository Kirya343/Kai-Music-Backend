package org.kirya343.features.audio.services.cache;

import java.util.HashSet;
import java.util.Set;

import org.kirya343.features.audio.datasource.model.AudioFile;
import org.kirya343.features.room.datasource.ListeningRoom;

import lombok.Getter;
import lombok.Setter;

@Getter
public class RoomPlaybackContext {

    public RoomPlaybackContext(
        Long roomId,
        AudioFile currentAudio,
        ListeningRoom room
    ) {
        this.listeners = new HashSet<>();
        this.currentAudio = currentAudio;
        this.room = room;
    }

    @Setter
    private Set<String> listeners;

    @Setter 
    private AudioFile currentAudio;

    @Setter 
    private ListeningRoom room;
}