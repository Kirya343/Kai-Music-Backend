package org.kirya343.features.audio.services.eventhandlers;

import org.kirya343.features.audio.services.cache.RoomPlaybackContext;
import org.kirya343.features.audio.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.audio.services.playback.RoomWebSocketService;
import org.kirya343.features.audio.services.streaming.AudioStreamWorkerManager;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.room.dto.RoomDTO;
import org.kirya343.features.user.dto.event.UserConnectedEvent;
import org.kirya343.features.user.dto.event.UserDisconnectedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioUserConnectionEventHandler {

    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final RoomWebSocketService roomWebSocketService;
    private final AudioStreamWorkerManager audioStreamWorkerManager;

    @Async 
    @EventListener
    @Transactional 
    public void handleConnected(UserConnectedEvent event) {
        
        ListeningRoom room = listeningRoomRepository
            .findRoomByUserId(event.authData().id())
            .orElse(null);
        
        if (room == null) return;

        roomPlaybackContextStore.computeIfAbsent(room.getId()).getListeners().add(event.authData().openId());

        RoomPlaybackContext roomContext = roomPlaybackContextStore.get(room.getId());

        roomWebSocketService.broadcastRoomInfo(event.authData().openId(), RoomDTO.ofRoomPlaybackContext(roomContext));
        audioStreamWorkerManager.getWorker(room.getId()).handleUserConnected(event.authData().openId());
    }

    @Async
    @EventListener
    public void handleDisconnected(UserDisconnectedEvent event) {
        
        ListeningRoom room = listeningRoomRepository
            .findRoomByUserId(event.authData().id())
            .orElse(null);

        if (room == null) return;

        roomPlaybackContextStore.computeIfAbsent(room.getId()).getListeners().remove(event.authData().openId());
        audioStreamWorkerManager.getWorker(room.getId()).handleUserDisconnected(event.authData().openId());
    }
}
