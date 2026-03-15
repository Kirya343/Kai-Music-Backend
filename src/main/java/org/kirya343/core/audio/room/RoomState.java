package org.kirya343.core.audio.room;

import java.util.concurrent.ScheduledFuture;

import lombok.Getter;
import lombok.Setter;

@Getter
public class RoomState {

    public RoomState(
        Long roomId,
        Long currentTrackId,
        long duration,
        long remaining,
        boolean paused
    ) {
        this.roomId = roomId;
        this.currentTrackId = currentTrackId;
        this.duration = duration;
        this.remaining = remaining;
        this.paused = paused;
    }

    private Long roomId;

    @Setter
    private Long currentTrackId;

    @Setter
    private long duration;

    @Setter
    private long remaining;

    @Setter
    private boolean paused;

    @Setter
    private ScheduledFuture<?> timer;

}