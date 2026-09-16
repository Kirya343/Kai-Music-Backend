package org.kirya343.features.audio.services.playback;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.audio.datasource.model.RoomPlaybackState;
import org.kirya343.features.audio.datasource.repository.RoomPlaybackStateRepository;
import org.kirya343.features.audio.dto.RoomPlaybackEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomUpdateEventHandler {

    private final RoomPlaybackStateRepository roomPlaybackStateRepository;
    private final EntityManager entityManager;

    @Async
    @EventListener
    @Transactional
    public void handlePlayback(RoomPlaybackEvent event) {
        
        RoomPlaybackState state = roomPlaybackStateRepository.findById(event.roomId())
            .orElse(new RoomPlaybackState(
                entityManager.getReference(ListeningRoom.class, event.roomId())
            ));

        state.setPosition(event.position());
        state.setCurrentQueueEntryId(event.audioId());
        state.setPaused(event.pause());
        state.setUser(event.authData().name());
        
        roomPlaybackStateRepository.save(state);
    }
}
