package org.kirya343.features.audio.dto.queue;

public record QueueItemCreateDTO(
    Long audioId,
    Integer position
) {
}
