package org.kirya343.features.audio.services.cache;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
public class RoomPlaybackContext {

    public RoomPlaybackContext(
        Long roomId,
        Long currentQueueEntryId,
        long duration,
        boolean paused
    ) {
        this.roomId = roomId;
        this.currentQueueEntryId = currentQueueEntryId;
        this.duration = duration;
        this.paused = paused;
        this.listeners = new HashSet<>();
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

    @Setter
    private Set<String> listeners;

    public long getPosition(long now) {

        long pos = now - resumedAt;
        
        long posInSeconds = pos / 1_000_000 / 1000;

        return posInSeconds;
    }
}