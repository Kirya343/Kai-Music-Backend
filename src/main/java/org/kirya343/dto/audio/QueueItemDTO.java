package org.kirya343.dto.audio;

public record QueueItemDTO(
    Long id,
    Long audioId,
    String name,
    Long position
) {
}
