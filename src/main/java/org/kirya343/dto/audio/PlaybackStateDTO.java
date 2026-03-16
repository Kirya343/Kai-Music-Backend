package org.kirya343.dto.audio;

public record PlaybackStateDTO(
    String user,
    Long audioId,
    Long position,
    boolean pause
) {
}
