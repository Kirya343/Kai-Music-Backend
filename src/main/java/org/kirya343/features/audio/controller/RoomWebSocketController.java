package org.kirya343.features.audio.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.features.audio.services.AudioQueryService;
import org.kirya343.features.audio.services.command.RoomCommandWorker;
import org.kirya343.features.audio.services.queue.QueueCommandService;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.audio.dto.queue.QueueItemCreateDTO;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.dto.RoomDTO;
import org.kirya343.features.room.dto.commands.Next;
import org.kirya343.features.room.dto.commands.Prev;
import org.kirya343.features.room.dto.commands.RoomCommand;
import org.kirya343.features.room.dto.commands.UpdatePlayback;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j 
@RequiredArgsConstructor
public class RoomWebSocketController {

    private final RoomCommandWorker roomCommandWorker;
    private final AudioQueryService audioQueryService;
    private final QueueCommandService queueCommandService;
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

        RoomCommand cmd = new UpdatePlayback(roomId, state, authData);
        
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

    @MessageMapping("/room/load")
    @SendToUser("/queue/room")
    public RoomDTO prev(
        @AuthenticationPrincipal UserAuthData authData
    ) {
        log.info("Catched /room/load");
        return audioQueryService.getCurrentRoom(authData);
    }

    @MessageMapping("/room/queue.add")
    public void addToQueue(
        List<QueueItemCreateDTO> list,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        queueCommandService.addQueueList(list, authData);
    }

    @MessageMapping("/room/queue.remove")
    public void removeFromQueue(
        @Payload List<Long> list,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        queueCommandService.removeQueueList(list, authData);
    }
}
