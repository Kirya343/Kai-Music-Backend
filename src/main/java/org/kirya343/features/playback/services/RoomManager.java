package org.kirya343.features.playback.services;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.dto.commands.UpdatePlayback;
import org.kirya343.features.playback.services.command.RoomCommandQueue;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomManager {

    private final RoomCommandQueue queue;

    public void updatePlayback(Long roomId, PlaybackStateDTO state, UserAuthData user) {
        queue.submit(new UpdatePlayback(roomId, state, user));
    }
}
