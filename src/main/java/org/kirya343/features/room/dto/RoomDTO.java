package org.kirya343.features.room.dto;

import java.util.List;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.audio.dto.AudioDTO;
import org.kirya343.features.playback.dto.queue.QueueItemDTO;
import org.kirya343.features.playback.enums.PlaybackMode;

public record RoomDTO(
    Long id,
    String title,
    Long ownerId,
    String code,
    Integer membersCount,
    PlaybackMode mode,
    List<QueueItemDTO> queue,
    AudioDTO audio
) {
    public static RoomDTO ofRoom(ListeningRoom room, AudioFile audio) {
        return new RoomDTO(
            room.getId(),
            room.getTitle() != null ? room.getTitle() : room.getOwner().getName() + "\'s room",
            room.getOwner().getId(),
            room.getCode(),
            room.getMembers().size(),
            room.getPlaybackMode(),
            QueueItemDTO.ofList(room.getQueue()),
            AudioDTO.ofAudioFile(audio)
        );
    }

    public static RoomDTO addAudioFile(RoomDTO r, AudioFile audio) {
        return new RoomDTO(
            r.id(),
            r.title(),
            r.ownerId(),
            r.code(),
            r.membersCount(),
            r.mode(),
            r.queue(),
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
            room.getPlaybackMode(),
            QueueItemDTO.ofList(room.getQueue()),
            null
        );
    }
}