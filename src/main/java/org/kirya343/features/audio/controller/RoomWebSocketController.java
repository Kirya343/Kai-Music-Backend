package org.kirya343.features.audio.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.features.audio.services.AudioQueryService;
import org.kirya343.features.audio.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.audio.services.command.RoomCommandWorker;
import org.kirya343.features.audio.datasource.model.AudioFile;
import org.kirya343.features.audio.datasource.model.QueueItem;
import org.kirya343.features.audio.datasource.repository.QueueItemRepository;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.room.dto.RoomDTO;
import org.kirya343.features.room.dto.commands.Next;
import org.kirya343.features.room.dto.commands.Prev;
import org.kirya343.features.room.dto.commands.RoomCommand;
import org.kirya343.features.room.dto.commands.UpdatePlayback;
import org.kirya343.features.user.datasource.User;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j 
@RequiredArgsConstructor
public class RoomWebSocketController {

    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final RoomCommandWorker roomCommandWorker;
    private final AudioQueryService audioQueryService;
    private final QueueItemRepository queueItemRepository;
    private final ListeningRoomRepository listeningRoomRepository;
    private final EntityManager entityManager;
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
        @Payload Long audioId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id()).orElseThrow();

        QueueItem qi = new QueueItem(
            room, 
            entityManager.getReference(AudioFile.class, audioId),
            queueItemRepository.findMaxPosition() + 50,
            entityManager.getReference(User.class, authData.id())
        );

        QueueItem saved = queueItemRepository.save(qi);
        log.info("new QueueItem {}", saved.getId());
        roomPlaybackContextStore.get(room.getId()).getRoom().getQueue().add(saved);

        ListeningRoom updated = listeningRoomRepository.findRoomByUserId(authData.id()).orElseThrow();

        roomPlaybackContextStore.reloadAndResendContext(updated);
    }

    @Transactional 
    @MessageMapping("/room/queue.remove")
    public void removeFromQueue(
        @Payload Long queueItemId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        int deleted = queueItemRepository.removeFromUserRoom(queueItemId, authData.id());

        if (deleted != 0) {
            ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id()).orElseThrow();

            roomPlaybackContextStore.reloadAndResendContext(room);
        }
    }
}
