package org.kirya343.dto.room.commands;

import org.kirya343.dto.auth.UserAuthData;

public record Prev(
    Long roomId,
    UserAuthData user
) implements RoomCommand {}