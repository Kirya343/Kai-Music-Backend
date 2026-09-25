package org.kirya343.features.playback.services.streaming;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.features.audio.services.storage.AudioStorageService;
import org.kirya343.features.playback.datasource.model.RoomPlaybackState;
import org.kirya343.features.playback.datasource.repository.RoomPlaybackStateRepository;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.services.RoomWebSocketService;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor
public class AudioStreamWorkerManager {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomWebSocketService roomWebSocketService;
    private final AudioStorageService audioStorageService;
    private final RoomPlaybackStateRepository roomPlaybackStateRepository;

    private final Map<Long, AudioStreamWorker> workers =
            new ConcurrentHashMap<>();

    public AudioStreamWorker getWorker(Long roomId) {

        log.info("Пытаемся получить воркер для комнаты {}", roomId);

        AudioStreamWorker worker = workers.get(roomId);

        if (worker == null) {

            RoomPlaybackState state = roomPlaybackStateRepository.findById(roomId).orElse(null);

            worker = new AudioStreamWorker(
                roomId, 
                roomPlaybackContextStore, 
                messagingTemplate, 
                eventPublisher, 
                roomWebSocketService, 
                audioStorageService, 
                PlaybackStateDTO.ofState(state)
            );

            workers.putIfAbsent(roomId, worker);
        }

        return worker;
    }

    public void removeWorker(Long roomId) {

        AudioStreamWorker worker = workers.remove(roomId);

        if (worker != null) {
            worker.cancelTask();
        }
    }
}