package org.kirya343.features.playback.dto;

import org.kirya343.features.playback.datasource.model.RoomPlaybackState;

public record PlaybackStateDTO(
    String user,
    Long entryId,
    Long position,
    boolean pause
) {

    public static PlaybackStateDTO ofState(RoomPlaybackState state) {

        if (state == null) return null;
        
        return new PlaybackStateDTO(
            state.getUser(), 
            state.getCurrentQueueEntryId(), 
            state.getCurrentPosition(), 
            state.isPaused()
        );
    }
}
