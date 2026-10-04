package org.kirya343.features.playback.eventhandlers;

import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playback.services.streaming.AudioStreamWorkerManager;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.user.dto.event.UserDisconnectedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioUserEventHandler {

    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final AudioStreamWorkerManager audioStreamWorkerManager;

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
