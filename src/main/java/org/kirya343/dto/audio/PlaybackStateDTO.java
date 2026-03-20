package org.kirya343.dto.audio;

public record PlaybackStateDTO(
    String user,
    Long entryId,
    Long position,
    boolean pause
) {
}
