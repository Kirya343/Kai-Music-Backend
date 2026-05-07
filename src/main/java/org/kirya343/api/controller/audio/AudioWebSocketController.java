package org.kirya343.api.controller.audio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.core.audio.room.RoomCommandWorker;
import org.kirya343.dto.audio.PlaybackStateDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.room.commands.Next;
import org.kirya343.dto.room.commands.Pause;
import org.kirya343.dto.room.commands.Play;
import org.kirya343.dto.room.commands.Prev;
import org.kirya343.dto.room.commands.RoomCommand;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AudioWebSocketController {

    private final RoomCommandWorker roomCommandWorker;
    private final Map<String, Long> lastUpdate = new ConcurrentHashMap<>();
    private static final long UPDATE_DELAY_MS = 300;

    private boolean shouldIgnore(Long userId, Long roomId, String action) {
        long now = System.currentTimeMillis();

        String key = userId + ":" + roomId + ":" + action;

        Long last = lastUpdate.get(key);

        if (last != null && now - last < UPDATE_DELAY_MS) {
            return true;
        }

        lastUpdate.put(key, now);
        return false;
    }
    
    @MessageMapping("/room/{roomId}/update-playback-state")
    public void updatePlaybackState(
        PlaybackStateDTO state,
        @DestinationVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        if (shouldIgnore(authData.id(), roomId, "updatePlayback")) {
            return;
        }

        RoomCommand cmd;

        if (!state.pause()) {
            cmd = new Play(roomId, state, authData);
        } else {
            cmd = new Pause(roomId, state, authData);
        }

        roomCommandWorker.submit(cmd);
    }

    @MessageMapping("/room/{roomId}/next")
    public void next(
        @DestinationVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        if (shouldIgnore(authData.id(), roomId, "next")) {
            return;
        }
        RoomCommand cmd = new Next(roomId, authData);
        roomCommandWorker.submit(cmd);
    }

    @MessageMapping("/room/{roomId}/prev")
    public void prev(
        @DestinationVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        if (shouldIgnore(authData.id(), roomId, "prev")) {
            return;
        }
        RoomCommand cmd = new Prev(roomId, authData);
        roomCommandWorker.submit(cmd);
    }
}
