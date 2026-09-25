package org.kirya343.features.playback.services.cache;

import java.util.HashSet;
import java.util.Set;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.dto.RoomDTO;

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
        this.room = RoomDTO.ofRoom(room);
    }

    @Setter
    private Set<String> listeners;

    @Setter 
    private AudioFile currentAudio;

    @Setter
    private RoomDTO room;

    public RoomDTO getFullRoom() {
        return RoomDTO.addAudioFile(room, currentAudio);
    }
}