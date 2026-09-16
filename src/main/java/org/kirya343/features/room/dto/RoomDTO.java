package org.kirya343.features.room.dto;

import java.util.List;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.audio.dto.QueueItemDTO;
import org.kirya343.features.audio.enums.PlaybackMode;

public class RoomDTO {
    
    public record Get(
        Long id,
        String title,
        Long ownerId,
        String code,
        Integer membersCount,
        PlaybackMode mode,
        List<QueueItemDTO> queue
    ) {
        public static RoomDTO.Get ofRoom(ListeningRoom room) {
            return new RoomDTO.Get(
                room.getId(),
                room.getTitle() != null ? room.getTitle() : room.getOwner().getName() + "\'s room",
                room.getOwner().getId(),
                room.getCode(),
                room.getMembers().size(),
                room.getPlaybackMode(),
                QueueItemDTO.ofList(room.getQueue())
            );
        }
    }
    public record Update(
        String title
    ) {
    }
}
