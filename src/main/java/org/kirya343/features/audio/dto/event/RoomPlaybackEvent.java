package org.kirya343.features.audio.dto.event;

import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.authentication.dto.UserAuthData;

public record RoomPlaybackEvent(
    Long roomId,
    PlaybackStateDTO stateDTO,
    UserAuthData authData
) {
}