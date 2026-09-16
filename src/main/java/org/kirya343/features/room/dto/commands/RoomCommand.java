package org.kirya343.features.room.dto.commands;

public sealed interface RoomCommand
    permits Play, Pause, Next, Prev, Tick {

    Long roomId();
}
