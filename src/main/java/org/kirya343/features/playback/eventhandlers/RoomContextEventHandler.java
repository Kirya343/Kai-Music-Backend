package org.kirya343.features.playback.eventhandlers;

import java.util.Set;

import org.kirya343.features.playback.dto.event.QueueChangedEvent;
import org.kirya343.features.playback.dto.event.RoomContextChangedEvent;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playlist.dto.PlaylistDTO;
import org.kirya343.features.playlist.service.PlaylistWebSocketService;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.room.services.RoomWebSocketService;
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
    private final PlaylistWebSocketService playlistWebSocketService;
    private final RoomWebSocketService roomWebSocketService;
    private final ListeningRoomRepository listeningRoomRepository;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQueueChanged(QueueChangedEvent event) {  
        
        ListeningRoom room = listeningRoomRepository.findByPlaylistId(event.playlistId()).orElse(null);

        if (room == null) return;

        Set<String> listners = roomPlaybackContextStore.get(room.getId()).getListeners();

        for (String user : listners) {
            playlistWebSocketService.broadcastPlaylist(user, PlaylistDTO.ofPlaylist(room.getPlaylist()));
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
