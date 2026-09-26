package org.kirya343.features.playback.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.dto.commands.Next;
import org.kirya343.features.playback.dto.commands.Prev;
import org.kirya343.features.playback.dto.commands.RoomCommand;
import org.kirya343.features.playback.dto.commands.UpdatePlayback;
import org.kirya343.features.playback.services.command.PlaybackCommandWorker;
import org.kirya343.features.user.dto.event.UserConnectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller 
@Slf4j 
@RequiredArgsConstructor 
public class PlaybackWebSocketController {

    private final PlaybackCommandWorker roomCommandWorker;
    private final Map<String, Long> lastUpdate = new ConcurrentHashMap<>();
    private static final long UPDATE_DELAY_MS = 300;
    private final ApplicationEventPublisher eventPublisher;
    
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
    
    @MessageMapping("/playback/{roomId}/update-playback-state")
    public void updatePlaybackState(
        PlaybackStateDTO state,
        @DestinationVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        if (shouldIgnore(authData.id(), roomId, "updatePlayback")) {
            return;
        }

        RoomCommand cmd = new UpdatePlayback(roomId, state, authData);
        
        roomCommandWorker.submit(cmd);
    }

    @MessageMapping("/playback/{roomId}/next")
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

    @MessageMapping("/playback/{roomId}/prev")
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

    @MessageMapping("/user.ready")
    public void updatePlaybackState(
        @AuthenticationPrincipal UserAuthData authData
    ) {

        eventPublisher.publishEvent(new UserConnectedEvent(authData));
    }
}
