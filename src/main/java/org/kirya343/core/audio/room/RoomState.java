package org.kirya343.core.audio.room;

import lombok.Getter;
import lombok.Setter;

@Getter
public class RoomState {

    public RoomState(
        Long roomId,
        Long currentQueueEntryId,
        long duration,
        boolean paused
    ) {
        this.roomId = roomId;
        this.currentQueueEntryId = currentQueueEntryId;
        this.duration = duration;
        this.paused = paused;
    }

    private Long roomId;

    @Setter
    private Long currentQueueEntryId;

    @Setter
    private long duration;

    @Setter
    private boolean paused;

    @Setter
    private long resumedAt;

    @Setter
    private long lastPosition;

    public long getPosition(long now) {

        long pos = now - resumedAt;
        
        long posInSeconds = pos / 1_000_000 / 1000;

        return posInSeconds;
    }

}