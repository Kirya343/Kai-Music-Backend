package org.kirya343.dto.audio;

import org.kirya343.dto.auth.UserAuthData;

public record RoomPlaybackEvent(
    Long roomId,
    Long audioId,
    Long position,
    boolean pause,
    UserAuthData authData
) {
}