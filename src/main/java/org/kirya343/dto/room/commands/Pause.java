package org.kirya343.dto.room.commands;

import org.kirya343.dto.audio.PlaybackStateDTO;
import org.kirya343.dto.auth.UserAuthData;

public record Pause(
    Long roomId,
    PlaybackStateDTO state,
    UserAuthData user
) implements RoomCommand {}
