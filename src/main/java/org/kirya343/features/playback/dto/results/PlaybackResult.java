package org.kirya343.features.playback.dto.results;

public sealed interface PlaybackResult
    permits Paused, Resumed, TrackChanged, NoOp {
}