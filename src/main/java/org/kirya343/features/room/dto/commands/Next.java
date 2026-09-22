package org.kirya343.features.room.dto.commands;

import org.kirya343.features.authentication.dto.UserAuthData;

public record Next(
    Long roomId,
    UserAuthData user
) implements RoomCommand {}