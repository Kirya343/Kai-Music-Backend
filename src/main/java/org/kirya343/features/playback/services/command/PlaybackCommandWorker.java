package org.kirya343.features.playback.services.command;

import java.util.concurrent.ThreadPoolExecutor;

import org.kirya343.features.audio.services.AudioQueryService;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.PlaybackWebsocketService;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.dto.commands.ChangeTrack;
import org.kirya343.features.playback.dto.commands.RoomCommand;
import org.kirya343.features.playback.dto.commands.UpdatePlayback;
import org.kirya343.features.playback.dto.event.RoomPlaybackEvent;
import org.kirya343.features.playback.services.QueueService;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playback.services.streaming.RoomStreamWorker;
import org.kirya343.features.playback.services.streaming.RoomStreamWorkerManager;
import org.kirya343.features.playlist.datasource.model.QueueItem;
import org.kirya343.features.playlist.datasource.repository.QueueItemRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor
public class PlaybackCommandWorker {

    private final ApplicationEventPublisher publisher;
    private final RoomPlaybackContextStore rooms;
    private final QueueService queueService;
    private final PlaybackWebsocketService webSocketService;
    private final PlaybackExecutorRegistry executorRegistry;
    private final RoomStreamWorkerManager roomStreamWorkerManager;
    private final AudioQueryService audioQueryService;
    private final QueueItemRepository queueItemRepository;

    public void submit(RoomCommand cmd) {

        ThreadPoolExecutor executor = executorRegistry.get(cmd.roomId());

        log.debug("Submit command {}", cmd.getClass());
        
        executor.submit(() -> handle(cmd));
    }

    private void handle(RoomCommand cmd) {
        RoomPlaybackContext roomContext = rooms.computeIfAbsent(cmd.roomId());

        UserAuthData authData = cmd.user();
        Long roomId = roomContext.getRoom().id();

        RoomStreamWorker streamWorker = roomStreamWorkerManager.getWorker(roomContext.getRoom().id());

        PlaybackStateDTO state = null;

        try {
            switch (cmd) {
                case UpdatePlayback c -> { 
                    
                    state = c.state();
                }
                case ChangeTrack c -> {

                    QueueItem entry = null;
                    
                    switch (c.changing()) {
                        case "prev":

                            entry = queueService.prevTrack(roomId);

                            break;

                        case "next":

                            entry = queueService.nextTrack(roomId);
                            
                            break;
                    
                        default:

                            try {

                                Long entryId = Long.parseLong(c.changing());

                                entry = queueItemRepository.findById(entryId).orElseThrow();

                            } catch (NumberFormatException e) {
                                throw e;
                            }
                            
                            break;
                    }

                    state = new PlaybackStateDTO(
                        cmd.user().name(), 
                        entry.getId(), 
                        0.0, 
                        false
                    );

                    roomContext.setCurrentAudio(
                        audioQueryService.getAudioByQueueItem(entry.getId())
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
