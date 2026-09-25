package org.kirya343.features.playback.dto.event;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.dto.PlaybackStateDTO;

public record RoomPlaybackEvent(
    Long roomId,
    PlaybackStateDTO stateDTO,
    UserAuthData authData
) {
}