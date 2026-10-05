package org.kirya343.features.playback.services.streaming;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.dto.commands.ChangeTrack;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.springframework.context.ApplicationEventPublisher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoomStreamWorker {

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();
    private final ApplicationEventPublisher eventPublisher;
    private final UserAudioStreamWorkerManager userAudioStreamWorkerManager;

    private final RoomPlaybackContext roomContext;

    private final Long roomId;

    private static final int CHUNK_INTERVAL_SECONDS = 2;

    private PlaybackStateDTO currentState;
    private ScheduledFuture<?> task;

    public RoomStreamWorker(
        Long roomId,
        ApplicationEventPublisher eventPublisher,
        UserAudioStreamWorkerManager userAudioStreamWorkerManager,
        PlaybackStateDTO currentState,
        RoomPlaybackContext roomContext
    ) {

        this.roomId = roomId;
        this.eventPublisher = eventPublisher;
        this.roomContext = roomContext;
        this.userAudioStreamWorkerManager = userAudioStreamWorkerManager;

        this.applyState(currentState);

        log.info(
            "Created AudioStreamWorker for room {}",
            roomId
        );
    }

    public void applyState(PlaybackStateDTO state) {

        log.info("applyState state={}", state == null);

        if (state == null) return;

        roomContext.getListeners().forEach(user -> {
            userAudioStreamWorkerManager.getWorker(user, state).applyState(state);
        });

        log.info("START STATE: entryId={}, position={}", state.entryId(), state.position());

        if (task == null || task.isCancelled()) {
            task = scheduler.scheduleAtFixedRate(
                () -> {
                    streamTick();
                },
                0,
                CHUNK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            );
        }
    }

    public void streamTick() {

        Set<String> listeners = roomContext.getListeners();

        /**
         * if current playback position is after than audio duration - cancel task and send Next command
         */
        if (currentState.position() > roomContext.getCurrentAudio().getDuration()) {

            log.debug("Sending NEXT event: room={}", roomId);
            eventPublisher.publishEvent(new ChangeTrack(roomId, "next", UserAuthData.server()));
            return;
        }

        try {
            listeners.forEach(user -> {
                try {
                    userAudioStreamWorkerManager.getWorker(user, currentState).tick();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!currentState.pause()) {
            currentState = new PlaybackStateDTO(
                currentState.user(), 
                currentState.entryId(), 
                currentState.position() + CHUNK_INTERVAL_SECONDS, 
                currentState.pause()
            );
        }
    }

    public void cancelTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            task = null;
        }
    }
}