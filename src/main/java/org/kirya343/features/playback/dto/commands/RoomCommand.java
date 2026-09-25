package org.kirya343.features.playback.dto.commands;

import org.kirya343.features.authentication.dto.UserAuthData;

public sealed interface RoomCommand
    permits UpdatePlayback, Next, Prev {

    Long roomId();
    UserAuthData user();
}
