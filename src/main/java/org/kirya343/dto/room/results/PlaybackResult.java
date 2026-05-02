package org.kirya343.dto.room.results;

public sealed interface PlaybackResult
    permits Paused, Resumed, TrackChanged, NoOp {
}