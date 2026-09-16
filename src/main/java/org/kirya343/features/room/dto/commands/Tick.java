package org.kirya343.features.room.dto.commands;

public record Tick(
    Long roomId,
    long now
) implements RoomCommand {}
