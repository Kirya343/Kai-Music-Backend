package org.kirya343.features.audio.services.playback;

import org.kirya343.features.audio.services.command.RoomCommandQueue;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.dto.commands.Pause;
import org.kirya343.features.room.dto.commands.Play;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomManager {

    private final RoomCommandQueue queue;

    public void play(Long roomId, PlaybackStateDTO state, UserAuthData user) {
        queue.submit(new Play(roomId, state, user));
    }

    public void pause(Long roomId, PlaybackStateDTO state, UserAuthData user) {
        queue.submit(new Pause(roomId, state, user));
    }
}
