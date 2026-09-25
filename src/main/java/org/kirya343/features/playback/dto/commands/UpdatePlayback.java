package org.kirya343.features.playback.dto.commands;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.dto.PlaybackStateDTO;

public record UpdatePlayback(
    Long roomId,
    PlaybackStateDTO state,
    UserAuthData user
) implements RoomCommand {}
