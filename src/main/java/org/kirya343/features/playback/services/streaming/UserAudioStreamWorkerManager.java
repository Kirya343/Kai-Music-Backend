package org.kirya343.features.playback.services.streaming;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kirya343.features.audio.services.AudioQueryService;
import org.kirya343.features.audio.services.storage.AudioStorageService;
import org.kirya343.features.playback.PlaybackWebsocketService;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Component 
@RequiredArgsConstructor 
public class UserAudioStreamWorkerManager {

    private final AudioQueryService audioQueryService;
    private final PlaybackWebsocketService playbackWebsocketService;
    private final AudioStorageService audioStorageService;

    private final Map<String, UserAudioStreamWorker> workers =
            new ConcurrentHashMap<>();

    public UserAudioStreamWorker getWorker(String userSub, PlaybackStateDTO currentState) {

        log.info("Пытаемся получить воркер для пользователя {}", userSub);

        UserAudioStreamWorker worker = workers.get(userSub);

        if (worker == null) {

            log.info("Создаём новый воркер для пользователя {}", userSub);

            worker = new UserAudioStreamWorker(
                userSub,
                audioQueryService,
                audioStorageService,
                playbackWebsocketService,
                currentState
            );

            log.info("Кладём к остальным воркер для пользователя {}", userSub);

            workers.putIfAbsent(userSub, worker);
        }

        return worker;
    }

    public void removeWorker(String userSub) {

        UserAudioStreamWorker worker = workers.remove(userSub);

        if (worker != null) {
            worker.cancelTask();
        }
    }
}
