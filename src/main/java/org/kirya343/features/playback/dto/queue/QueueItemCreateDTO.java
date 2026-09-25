package org.kirya343.features.playback.dto.queue;

public record QueueItemCreateDTO(
    Long audioId,
    Integer position
) {
}
