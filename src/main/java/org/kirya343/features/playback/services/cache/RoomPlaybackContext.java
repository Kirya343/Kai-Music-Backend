package org.kirya343.features.playback.services.cache;

import java.util.HashSet;
import java.util.Set;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.room.dto.ShortRoomDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
public class RoomPlaybackContext {

    public RoomPlaybackContext(
        Long roomId,
        AudioFile currentAudio,
        ShortRoomDTO room
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
    private ShortRoomDTO room;
}