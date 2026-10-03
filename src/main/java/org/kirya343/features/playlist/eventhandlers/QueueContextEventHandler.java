package org.kirya343.features.playlist.eventhandlers;

import org.kirya343.features.playback.dto.event.QueueChangedEvent;
import org.kirya343.features.playlist.datasource.model.Playlist;
import org.kirya343.features.playlist.datasource.repository.PlaylistRepository;
import org.kirya343.features.playlist.dto.PlaylistDTO;
import org.kirya343.features.playlist.service.PlaylistWebSocketService;
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
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQueueChanged(QueueChangedEvent event) {      
        Playlist playlist = playlistRepository.findById(event.playlistId()).orElse(null);

        playlistWebSocketService.broadcastPlaylist(playlist.getOwner().getOpenId(), PlaylistDTO.ofPlaylist(playlist));
    }
}
