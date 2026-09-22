package org.kirya343.features.room.dto.results;

public sealed interface PlaybackResult
    permits Paused, Resumed, TrackChanged, NoOp {
}