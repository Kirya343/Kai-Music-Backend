package org.kirya343.features.room.dto;

import java.util.List;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.audio.datasource.model.AudioFile;
import org.kirya343.features.audio.dto.AudioDTO;
import org.kirya343.features.audio.dto.queue.QueueItemDTO;
import org.kirya343.features.audio.enums.PlaybackMode;
import org.kirya343.features.audio.services.cache.RoomPlaybackContext;

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

    public static RoomDTO ofRoomPlaybackContext(RoomPlaybackContext context) {

        ListeningRoom room = context.getRoom();

        return new RoomDTO(
            context.getRoom().getId(),
            room.getTitle() != null ? room.getTitle() : room.getOwner().getName() + "\'s room",
            room.getOwner().getId(),
            room.getCode(),
            context.getListeners().size(),
            room.getPlaybackMode(),
            QueueItemDTO.ofList(room.getQueue()),
            AudioDTO.ofAudioFile(context.getCurrentAudio())
        );
    }
}