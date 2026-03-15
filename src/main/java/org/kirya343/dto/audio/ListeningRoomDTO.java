package org.kirya343.dto.audio;

import java.util.List;

import org.kirya343.enums.PlaybackMode;

public record ListeningRoomDTO(
    Long id,
    String title,
    Long ownerId,
    Integer membersCount,
    PlaybackMode mode,
    List<QueueItemDTO> queue
) {
}
