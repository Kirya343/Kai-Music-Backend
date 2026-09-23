package org.kirya343.features.audio.services.eventhandlers;

import org.kirya343.features.audio.datasource.model.RoomPlaybackState;
import org.kirya343.features.audio.datasource.repository.RoomPlaybackStateRepository;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.audio.dto.event.RoomContextCreatedEvent;
import org.kirya343.features.audio.services.streaming.AudioStreamWorker;
import org.kirya343.features.audio.services.streaming.AudioStreamWorkerManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j 
@RequiredArgsConstructor
public class RoomContextEventHandler {

    private final AudioStreamWorkerManager audioStreamWorkerManager;
    private final RoomPlaybackStateRepository roomPlaybackStateRepository;

    @EventListener
    public void handleContextCreated(RoomContextCreatedEvent event) {
        
        RoomPlaybackState state = roomPlaybackStateRepository.findById(event.roomId()).orElseThrow();

        AudioStreamWorker worker = audioStreamWorkerManager.getWorker(event.roomId());
        
        if (worker != null) {
            worker.applyState(PlaybackStateDTO.ofState(state));
        }
    }
}
