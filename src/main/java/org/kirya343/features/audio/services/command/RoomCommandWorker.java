package org.kirya343.features.audio.services.command;

import java.util.concurrent.ThreadPoolExecutor;

import org.kirya343.features.audio.services.cache.RoomPlaybackContext;
import org.kirya343.features.audio.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.audio.services.playback.QueueService;
import org.kirya343.features.audio.services.playback.RoomWebSocketService;
import org.kirya343.features.audio.services.streaming.AudioStreamWorker;
import org.kirya343.features.audio.services.streaming.AudioStreamWorkerManager;
import org.kirya343.features.audio.datasource.model.QueueItem;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.audio.dto.event.RoomPlaybackEvent;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.dto.commands.Next;
import org.kirya343.features.room.dto.commands.Prev;
import org.kirya343.features.room.dto.commands.RoomCommand;
import org.kirya343.features.room.dto.commands.UpdatePlayback;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor
public class RoomCommandWorker {

    private final ApplicationEventPublisher publisher;
    private final RoomPlaybackContextStore rooms;
    private final QueueService queueService;
    private final RoomWebSocketService webSocketService;
    private final RoomExecutorRegistry executorRegistry;
    private final AudioStreamWorkerManager audioStreamWorkerManager;

    public void submit(RoomCommand cmd) {

        ThreadPoolExecutor executor = executorRegistry.get(cmd.roomId());

        log.debug("Submit command {}", cmd.getClass());
        
        executor.submit(() -> handle(cmd));
    }

    private void handle(RoomCommand cmd) {
        RoomPlaybackContext roomContext = rooms.computeIfAbsent(cmd.roomId());

        UserAuthData authData = cmd.user();
        Long roomId = roomContext.getRoom().id();

        AudioStreamWorker streamWorker = audioStreamWorkerManager.getWorker(roomContext.getRoom().id());

        PlaybackStateDTO state = null;

        switch (cmd) {
            case UpdatePlayback c -> { 
                
                state = c.state();
            }
            case Next c -> {
                QueueItem entry = queueService.nextTrack(roomId);

                state = new PlaybackStateDTO(
                    cmd.user().name(), 
                    entry.getId(), 
                    Long.valueOf(0), 
                    false
                );
            }
            case Prev c -> {

                QueueItem entry = queueService.nextTrack(roomId);

                state = new PlaybackStateDTO(
                    cmd.user().name(), 
                    entry.getId(), 
                    Long.valueOf(0), 
                    false
                );
            }
            default -> throw new RuntimeException("Введена неверная команда");
        };

        streamWorker.applyState(state);

        publisher.publishEvent(
            new RoomPlaybackEvent(
                roomContext.getRoom().id(), 
                state, 
                authData
            ));

        for (String user : roomContext.getListeners()) {
            webSocketService.broadcastPlaybackState(user, state);
        }
    }
}
