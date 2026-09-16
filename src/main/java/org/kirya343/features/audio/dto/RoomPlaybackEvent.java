package org.kirya343.features.audio.dto;

import org.kirya343.features.authentication.dto.UserAuthData;

public record RoomPlaybackEvent(
    Long roomId,
    Long audioId,
    Long position,
    boolean pause,
    UserAuthData authData
) {
}