package org.kirya343.features.playback.dto.results;

import org.kirya343.features.authentication.dto.UserAuthData;

public record Resumed(
    Long roomId,
    Long trackId,
    long position,
    UserAuthData user
) implements PlaybackResult {}