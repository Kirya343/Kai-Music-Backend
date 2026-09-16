package org.kirya343.features.audio.services.eventhandlers;

import org.kirya343.features.audio.services.cache.RoomPlaybackStateStore;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.user.dto.event.UserConnectedEvent;
import org.kirya343.features.user.dto.event.UserDisconnectedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioUserConnectionEventHandler {

    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomPlaybackStateStore roomPlaybackStateStore;

    @EventListener
    public void handleConnected(UserConnectedEvent event) {
        
        ListeningRoom room = listeningRoomRepository
            .findUserListeningRoom(event.authData().id())
            .orElseThrow();

        roomPlaybackStateStore.get(room.getId()).getListeners().add(event.authData().openId());
    }

    @EventListener
    public void handleDisconnected(UserDisconnectedEvent event) {
        
        ListeningRoom room = listeningRoomRepository
            .findUserListeningRoom(event.authData().id())
            .orElseThrow();

        roomPlaybackStateStore.get(room.getId()).getListeners().remove(event.authData().openId());
    }
}
