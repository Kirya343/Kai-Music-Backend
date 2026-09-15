package org.kirya343.core.audio.streaming;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.datasource.repository.audio.AudioFileRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AudioStreamWorkerManager {

    private final AudioFileRepository audioFileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<Long, AudioStreamWorker> workers =
            new ConcurrentHashMap<>();

    public AudioStreamWorker getWorker(Long roomId) {

        return workers.computeIfAbsent(
                roomId,
                id -> new AudioStreamWorker(
                        id,
                        audioFileRepository,
                        messagingTemplate
                )
        );
    }

    public void removeWorker(Long roomId) {

        AudioStreamWorker worker = workers.remove(roomId);

        if (worker != null) {
            worker.stop();
        }
    }
}