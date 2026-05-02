package org.kirya343.dto.room.results;

import org.kirya343.dto.auth.UserAuthData;

public record TrackChanged(
    Long roomId,
    Long trackId,
    UserAuthData user
) implements PlaybackResult {}
