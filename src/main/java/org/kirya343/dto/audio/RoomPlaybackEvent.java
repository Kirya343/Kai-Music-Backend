package org.kirya343.dto.audio;

public record RoomPlaybackEvent(
    Long roomId,
    Long audioId,
    Long position,
    boolean pause
) {
}