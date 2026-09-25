package org.kirya343.features.playback.eventhandlers;

import org.kirya343.features.playback.services.RoomSessionService;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playback.services.streaming.AudioStreamWorkerManager;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.user.dto.event.UserConnectedEvent;
import org.kirya343.features.user.dto.event.UserDisconnectedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioUserEventHandler {

    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final AudioStreamWorkerManager audioStreamWorkerManager;
    private final RoomSessionService roomSessionService;

    @Async 
    @EventListener
    @Transactional 
    public void handleConnected(UserConnectedEvent event) {
        
        ListeningRoom room = listeningRoomRepository
            .findRoomByUserId(event.authData().id())
            .orElse(null);
        
        if (room == null) return;

        roomSessionService.initializeRoom(room.getId(), event.authData());
    }

    @Async
    @EventListener
    public void handleDisconnected(UserDisconnectedEvent event) {
        
        ListeningRoom room = listeningRoomRepository
            .findRoomByUserId(event.authData().id())
            .orElse(null);

        if (room == null) return;

        roomPlaybackContextStore.get(room.getId()).getListeners().remove(event.authData().openId());
        audioStreamWorkerManager.getWorker(room.getId()).handleUserDisconnected(event.authData().openId());

        roomPlaybackContextStore.listenersUpdated(room.getId());
    }
}
