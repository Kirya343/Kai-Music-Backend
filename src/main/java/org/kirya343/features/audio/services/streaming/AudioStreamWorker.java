package org.kirya343.features.audio.services.streaming;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kirya343.features.audio.services.cache.RoomPlaybackContext;
import org.kirya343.features.audio.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.audio.services.playback.RoomWebSocketService;
import org.kirya343.features.audio.services.storage.AudioStorageService;
import org.kirya343.features.audio.services.util.Fmp4Chunker;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.dto.commands.Next;
import org.kirya343.features.audio.datasource.model.AudioFile;
import org.kirya343.features.audio.dto.AudioChunk;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AudioStreamWorker {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();
    private final ApplicationEventPublisher eventPublisher;
    private final RoomWebSocketService roomWebSocketService;
    private final AudioStorageService audioStorageService;

    private final Long roomId;

    private static final int CHUNK_INTERVAL_SECONDS = 3;

    private PlaybackStateDTO currentState;
    private Set<String> initializedListeners = new HashSet<>();
    private ScheduledFuture<?> task;

    private static final int PAUSED_BUFFER_SECONDS = 15;

    private final Map<String, Fmp4Chunker> userChunkers = new HashMap<>();
    private final Map<String, Double> bufferedUntil = new HashMap<>();

    public AudioStreamWorker(
        Long roomId,
        RoomPlaybackContextStore roomPlaybackContextStore,
        SimpMessagingTemplate messagingTemplate,
        ApplicationEventPublisher eventPublisher,
        RoomWebSocketService roomWebSocketService,
        AudioStorageService audioStorageService,
        PlaybackStateDTO currentState
    ) {

        this.roomId = roomId;
        this.roomPlaybackContextStore = roomPlaybackContextStore;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.roomWebSocketService = roomWebSocketService;
        this.audioStorageService = audioStorageService;
        this.currentState = currentState;

        log.info(
            "Created AudioStreamWorker for room {}",
            roomId
        );
    }

    private AudioFile getAudioFile() {
        return roomPlaybackContextStore
            .computeIfAbsent(roomId)
            .getCurrentAudio();
    }

    public void applyState(PlaybackStateDTO state) {

        updateState(state);

        log.info("START STATE: entryId={}, position={}", state.entryId(), state.position());

        if (task == null || task.isCancelled()) {
            task = scheduler.scheduleAtFixedRate(
                this::streamTick,
                0,
                CHUNK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            );
        }
    }

    public void streamTick() {
        Set<String> listeners = roomPlaybackContextStore.computeIfAbsent(roomId).getListeners();

        /**
         * if current playback position is after than audio duration - cancel task and send Next command
         */
        if (currentState.position() > getAudioFile().getDuration()) {

            log.debug("Sending NEXT event: room={}", roomId);
            eventPublisher.publishEvent(new Next(roomId, UserAuthData.server()));
            return;
        }

        for (String user : listeners) {

            Fmp4Chunker chunker = getChunker(user, currentState);

            try {
                if (!initializedListeners.contains(user)) {
                    sendChunk(user, chunker.initializationChunk());
                    initializedListeners.add(user);
                }

                if (chunker.hasNext()) {
                    if (currentState.pause()) {
                        double target = currentState.position() + PAUSED_BUFFER_SECONDS;

                        if (bufferedUntil.getOrDefault(user, 0.0) < target) {

                            AudioChunk chunk = chunker.nextAudioChunk();

                            sendChunk(user, chunk);

                            /**
                            * that formule counts time summ of prev chunks with current chunk
                            * and multiple it with base chunk-duration 
                            **/
                            bufferedUntil.put(
                                user,
                                (chunk.sequence() + 2) * (chunk.durationMs() / 1000.0)
                            );
                        }
                    } else {

                        AudioChunk chunk = chunker.nextAudioChunk();

                        sendChunk(user, chunk);
                    }
                }

            } catch (IOException e) {

            // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (!currentState.pause()) {
            currentState = new PlaybackStateDTO(
                currentState.user(), 
                currentState.entryId(), 
                currentState.position() + 3, 
                currentState.pause()
            );
        }
    }

    private void sendChunk(String user, AudioChunk chunk) {
        Map<String, Object> headers = new HashMap<>();

        log.debug(
            "Sending {} chunk to {}: chunk={}, audio={}", 
            chunk.initialization() ? "initialization" : "media", 
            user, 
            chunk.sequence(),
            getAudioFile().getName());

        headers.put("content-type", "audio/mp4");
        headers.put("sequence", chunk.sequence());
        headers.put("duration", chunk.durationMs());
        headers.put("initialization", chunk.initialization());

        messagingTemplate.convertAndSendToUser(
            user,
            "/queue/audio",
            chunk.data(),
            headers
        );
    }

    public void updateState(PlaybackStateDTO state) {

        PlaybackStateDTO previousState = currentState;

        boolean trackChanged =
            currentState != null &&
            !currentState.entryId().equals(state.entryId());
        
        boolean positionChanged =
            currentState != null &&
            !currentState.position().equals(state.position());

        currentState = state;

        /**
         * if track is changed - clear all users and user chunkers,
         * becouse for new track audioStreamWorker have to create new parser with new track
         * and recreate all user chunkers with new parser
         */
        if (trackChanged) {
            log.info(
                "SWITCH TRACK: {} => {}",
                previousState.entryId(),
                currentState.entryId()
            );

            initializedListeners.clear();
            userChunkers.clear();

            roomPlaybackContextStore.updateRoomAudio(roomId, currentState);

            RoomPlaybackContext context =
                roomPlaybackContextStore.get(roomId);

            try {
                for (String user : context.getListeners()) {
                    getChunker(user, currentState);
                }
            } catch (Exception e) {
                log.info("Exception {}", e);
            }
        }

        if (positionChanged) {
            log.info(
                "SEEK POSITION: {} => {}",
                previousState.entryId(),
                currentState.entryId()
            );

            for (String user : initializedListeners) {
                Fmp4Chunker chunker = getChunker(user, currentState);

                chunker.seek(currentState.position());
                bufferedUntil.remove(user);
            }
        }
    }

    private Fmp4Chunker getChunker(String user, PlaybackStateDTO stateDTO) {
        Fmp4Chunker chunker = userChunkers.computeIfAbsent(
                user,
                ignored -> {
                    Fmp4Chunker newChunker = null;
                    try {
                        newChunker = new Fmp4Chunker(
                            audioStorageService,
                            getAudioFile().getPath(),
                            getAudioFile().getChunks()
                        );
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    log.debug("Перематываем чанкер для пользователя {} на позицию: {}", user, stateDTO.position());

                    newChunker.seek(stateDTO.position());
                    return newChunker;
                }
            );
        return chunker;
    }

    public void handleUserDisconnected(String user) {
        this.initializedListeners.remove(user);
        this.bufferedUntil.remove(user);
        this.userChunkers.remove(user);
    }

    public void handleUserConnected(String user) {
        log.info("user connected and recive {}", currentState.position());
        roomWebSocketService.broadcastPlaybackState(user, currentState);
    }

    public void cancelTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            task = null;
        }
    }
}