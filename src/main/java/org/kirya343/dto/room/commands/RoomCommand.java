package org.kirya343.dto.room.commands;

public sealed interface RoomCommand
    permits Play, Pause, Next, Prev, Tick {

    Long roomId();
}
