package org.kirya343.features.playback.services.streaming;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.features.playback.datasource.model.RoomPlaybackState;
import org.kirya343.features.playback.datasource.repository.RoomPlaybackStateRepository;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.presence.services.PresenceService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor
public class RoomStreamWorkerManager {

    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomPlaybackStateRepository roomPlaybackStateRepository;
    private final UserAudioStreamWorkerManager userAudioStreamWorkerManager;
    private final PresenceService presenceService;

    private final Map<Long, RoomStreamWorker> workers =
            new ConcurrentHashMap<>();

    public boolean checkWorker(Long roomId) {
        return workers.get(roomId) != null;
    }

    public RoomStreamWorker getWorker(Long roomId) {

        log.debug("Пытаемся получить воркер для комнаты {}", roomId);

        RoomStreamWorker worker = workers.get(roomId);

        if (worker == null) {

            log.debug("Создаём новый воркер для комнаты {}", roomId);

            RoomPlaybackState state = roomPlaybackStateRepository.findById(roomId).orElse(null);

            RoomPlaybackContext context = roomPlaybackContextStore.computeIfAbsent(roomId);

            try {
                worker = new RoomStreamWorker(
                    roomId,
                    eventPublisher, 
                    userAudioStreamWorkerManager,
                    PlaybackStateDTO.ofState(state),
                    context,
                    presenceService
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            log.debug("Кладём к остальным воркер для комнаты {}", roomId);

            workers.putIfAbsent(roomId, worker);
        }

        return worker;
    }

    public void removeWorker(Long roomId) {

        RoomStreamWorker worker = workers.remove(roomId);

        if (worker != null) {
            worker.cancelTask();
        }
    }
}