package org.kirya343.features.playlist.dto.queue;

public record QueueItemCreateDTO(
    Long audioId,
    Integer position
) {
}
