package org.kirya343.dto.room.commands;

public record Tick(
    Long roomId,
    long now
) implements RoomCommand {}
