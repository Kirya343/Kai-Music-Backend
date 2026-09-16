package org.kirya343.features.audio.dto;

public record PlaybackStateDTO(
    String user,
    Long entryId,
    Long position,
    boolean pause
) {
}
