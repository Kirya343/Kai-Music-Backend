package org.kirya343.core.audio.room;

import org.kirya343.dto.audio.PlaybackStateDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.room.commands.Pause;
import org.kirya343.dto.room.commands.Play;
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
