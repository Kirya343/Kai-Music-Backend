package org.kirya343.dto.audio;

public record PlaybackStateDTO(
    Long audioId,
    Long position,
    boolean pause
) {
}
