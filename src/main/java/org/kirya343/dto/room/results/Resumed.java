package org.kirya343.dto.room.results;

import org.kirya343.dto.auth.UserAuthData;

public record Resumed(
    Long roomId,
    Long trackId,
    long position,
    UserAuthData user
) implements PlaybackResult {}