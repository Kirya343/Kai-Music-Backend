package org.kirya343.features.playlist.eventhandlers;

import org.kirya343.features.playback.dto.event.QueueChangedEvent;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playlist.datasource.model.Playlist;
import org.kirya343.features.playlist.datasource.repository.PlaylistRepository;
import org.kirya343.features.playlist.dto.PlaylistDTO;
import org.kirya343.features.playlist.service.PlaylistWebSocketService;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor
public class QueueContextEventHandler {

    private final PlaylistWebSocketService playlistWebSocketService;
    private final PlaylistRepository playlistRepository;
    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQueueChanged(QueueChangedEvent event) {      
        Playlist playlist = playlistRepository.findById(event.playlistId()).orElse(null);

        playlistWebSocketService.broadcastPlaylist(playlist.getOwner().getOpenId(), PlaylistDTO.ofPlaylist(playlist));

        ListeningRoom room = listeningRoomRepository.findByPlaylistId(event.playlistId()).orElse(null);

        if (room != null) {
            RoomPlaybackContext context = roomPlaybackContextStore.get(room.getId());

            if (context != null) {
                context.getListeners().forEach(user -> 
                    playlistWebSocketService.broadcastRoomPlaylist(
                        user, 
                        PlaylistDTO.ofPlaylist(playlist)
                    ));
            }
        }
    }
}
