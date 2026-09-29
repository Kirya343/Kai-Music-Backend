package org.kirya343.features.playback.dto.commands;

import org.kirya343.features.authentication.dto.UserAuthData;

public record ChangeTrack(
    Long roomId,
    String changing,
    UserAuthData user
) implements RoomCommand {}