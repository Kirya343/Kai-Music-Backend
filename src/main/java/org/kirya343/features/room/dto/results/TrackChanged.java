package org.kirya343.features.room.dto.results;

import org.kirya343.features.authentication.dto.UserAuthData;

public record TrackChanged(
    Long roomId,
    Long trackId,
    UserAuthData user
) implements PlaybackResult {}
