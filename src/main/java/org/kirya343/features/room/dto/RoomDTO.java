package org.kirya343.features.room.dto;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.audio.dto.AudioDTO;
import org.kirya343.features.playlist.dto.PlaylistDTO;

public record RoomDTO(
    Long id,
    String title,
    Long ownerId,
    String code,
    Integer listeners,
    PlaylistDTO playlist,
    AudioDTO audio
) {
    public static RoomDTO ofRoom(ListeningRoom room, AudioFile audio) {
        return new RoomDTO(
            room.getId(),
            room.getTitle() != null ? room.getTitle() : room.getOwner().getName() + "\'s room",
            room.getOwner().getId(),
            room.getCode(),
            room.getMembers().size(),
            PlaylistDTO.ofPlaylist(room.getPlaylist()),
            AudioDTO.ofAudioFile(audio)
        );
    }

    public static RoomDTO byContext(RoomDTO r, AudioFile audio, Integer listeners) {
        return new RoomDTO(
            r.id(),
            r.title(),
            r.ownerId(),
            r.code(),
            listeners,
            r.playlist(),
            AudioDTO.ofAudioFile(audio)
        );
    }

    public static RoomDTO ofRoom(ListeningRoom room) {

        return new RoomDTO(
            room.getId(),
            room.getTitle() != null ? room.getTitle() : room.getOwner().getName() + "\'s room",
            room.getOwner().getId(),
            room.getCode(),
            room.getMembers().size(),
            PlaylistDTO.ofPlaylist(room.getPlaylist()),
            null
        );
    }
}