package org.kirya343.dto.audio;

import java.util.List;

public record ListeningRoomDTO(
    Long id,
    String title,
    Long ownerId,
    Integer membersCount,
    List<QueueItemDTO> queue
) {
}
