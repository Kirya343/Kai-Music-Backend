package org.kirya343.features.playback.eventhandlers;

import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playback.services.streaming.UserAudioStreamWorkerManager;
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
    private final UserAudioStreamWorkerManager userAudioStreamWorkerManager;

    @Async
    @EventListener
    public void handleDisconnected(UserDisconnectedEvent event) {
        
        ListeningRoom room = listeningRoomRepository
            .findRoomByUserId(event.authData().id())
            .orElse(null);

        if (room == null) return;

        RoomPlaybackContext context = roomPlaybackContextStore.get(room.getId());

        if (context != null) {
            context.getListeners().remove(event.authData().openId());
            roomPlaybackContextStore.listenersUpdated(room.getId());
        }

        userAudioStreamWorkerManager.removeWorker(event.authData().openId());
    }
}
