package org.kirya343.features.playback.eventhandlers;

import java.util.Set;

import org.kirya343.features.playback.dto.event.QueueChangedEvent;
import org.kirya343.features.playback.dto.event.RoomContextChangedEvent;
import org.kirya343.features.playback.services.RoomWebSocketService;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playlist.datasource.model.Playlist;
import org.kirya343.features.playlist.datasource.repository.PlaylistRepository;
import org.kirya343.features.playlist.dto.PlaylistDTO;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor
public class RoomContextEventHandler {

    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final RoomWebSocketService roomWebSocketService;
    private final PlaylistRepository playlistRepository;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQueueChanged(QueueChangedEvent event) {      

        Set<String> listners = roomPlaybackContextStore.get(event.roomId()).getListeners();

        Playlist playlist = playlistRepository.findByRoomId(event.roomId()).orElse(null);

        if (playlist != null) {
            for (String user : listners) {
                roomWebSocketService.broadcastPlaylist(user, PlaylistDTO.ofPlaylist(playlist));
            }
        }
    }

    @EventListener
    public void handleContextChanged(RoomContextChangedEvent event) {      

        RoomPlaybackContext roomContext = roomPlaybackContextStore.get(event.roomId());

        for (String user : roomContext.getListeners()) {
            roomWebSocketService.broadcastRoomInfo(user, roomContext.getRoom());
        }
    }
}
