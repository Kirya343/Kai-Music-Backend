package org.kirya343.dto.audio;

import java.util.List;

import org.kirya343.enums.PlaybackMode;

public class RoomDTO {
    
    public record Get(
        Long id,
        String title,
        Long ownerId,
        Integer membersCount,
        PlaybackMode mode,
        List<QueueItemDTO> queue
    ) {
    }

    public record Update(
        String title
    ) {
    }
}
