package org.kirya343.features.playback.services.streaming;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kirya343.features.audio.services.storage.AudioStorageService;
import org.kirya343.features.audio.services.util.Fmp4Chunker;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.PlaybackWebsocketService;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.dto.commands.ChangeTrack;
import org.kirya343.features.playback.services.RoomWebSocketService;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.audio.dto.AudioChunk;
import org.kirya343.features.audio.dto.AudioDTO;
import org.springframework.context.ApplicationEventPublisher;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AudioStreamWorker {

    private final PlaybackWebsocketService playbackWebsocketService;
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();
    private final ApplicationEventPublisher eventPublisher;
    private final RoomWebSocketService roomWebSocketService;
    private final AudioStorageService audioStorageService;

    private final RoomPlaybackContext roomContext;

    private final Long roomId;

    private static final int CHUNK_INTERVAL_SECONDS = 2;

    private PlaybackStateDTO currentState;
    private Set<String> initializedListeners = new HashSet<>();
    private ScheduledFuture<?> task;

    private static final int BUFFER_SECONDS = 20;

    private final Map<String, Fmp4Chunker> userChunkers = new HashMap<>();
    private final Map<String, Double> bufferedUntil = new HashMap<>();

    public AudioStreamWorker(
        Long roomId,
        PlaybackWebsocketService playbackWebsocketService,
        ApplicationEventPublisher eventPublisher,
        RoomWebSocketService roomWebSocketService,
        AudioStorageService audioStorageService,
        PlaybackStateDTO currentState,
        RoomPlaybackContext roomContext
    ) {

        this.roomId = roomId;
        this.playbackWebsocketService = playbackWebsocketService;
        this.eventPublisher = eventPublisher;
        this.roomWebSocketService = roomWebSocketService;
        this.audioStorageService = audioStorageService;
        this.roomContext = roomContext;

        this.applyState(currentState);

        log.info(
            "Created AudioStreamWorker for room {}",
            roomId
        );
    }

    public void applyState(PlaybackStateDTO state) {

        log.info("applyState state={}", state == null);

        if (state == null) return;

        updateState(state);

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
            for (String user : listeners) {
                userTick(user);
            }
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

    private void userTick(String user) throws IOException {
        Fmp4Chunker chunker = getChunker(user, currentState);

        log.debug("Tick to user {}", user);

        if (!initializedListeners.contains(user)) {
            initializeUser(user);
        }

        if (chunker.hasNext()) {
            double target = currentState.position() + BUFFER_SECONDS;

            if (bufferedUntil.getOrDefault(user, 0.0) < target) {

                AudioChunk chunk = chunker.nextAudioChunk();

                playbackWebsocketService.sendChunk(
                    user, 
                    chunk, 
                    currentState.entryId());

                /**
                * that formule counts time summ of prev chunks with current chunk
                * and multiple it with base chunk-duration 
                **/
                double chunkDurationS = chunk.durationMs() / 1000.0;
                double chunkDurationUntil = (chunk.sequence() + 1) * chunkDurationS; 

                bufferedUntil.put(user, chunkDurationUntil);
            }
        }
    }

    private void initializeUser(String user) throws IOException {

        Fmp4Chunker chunker = getChunker(user, currentState);

        playbackWebsocketService.sendChunk(
            user, 
            chunker.initializationChunk(), 
            currentState.entryId());

        bufferedUntil.put(user, currentState.position());

        initializedListeners.add(user);
    }

    public void updateState(PlaybackStateDTO state) {

        PlaybackStateDTO previousState = currentState;

        boolean trackChanged =
            currentState == null || (
                currentState != null &&
                !currentState.entryId().equals(state.entryId())
            );
        
        boolean positionChanged =
            currentState == null || (
                currentState != null &&
                !currentState.position().equals(state.position())
            );

        currentState = state;

        /**
         * if track is changed - clear all users and user chunkers,
         * becouse for new track audioStreamWorker have to create new parser with new track
         * and recreate all user chunkers with new parser
         */
        if (trackChanged) {
            log.info(
                "SWITCH TRACK: {} => {}",
                previousState != null ? previousState.entryId() : "null",
                currentState.entryId()
            );

            initializedListeners.clear();
            userChunkers.clear();
            bufferedUntil.clear();

            try {
                for (String user : roomContext.getListeners()) {
                    getChunker(user, currentState);
                    roomWebSocketService.broadcastAudioInfo(user, AudioDTO.ofAudioFile(roomContext.getCurrentAudio()));
                }
            } catch (Exception e) {
                log.info("Exception {}", e);
            }
        }

        if (positionChanged) {
            log.info(
                "SEEK POSITION: {} => {}",
                previousState != null ? previousState.position() : "null",
                currentState.position()
            );

            for (String user : initializedListeners) {
                Fmp4Chunker chunker = getChunker(user, currentState);

                chunker.seek(currentState.position());
                bufferedUntil.put(user, currentState.position());
            }
        }
    }

    private Fmp4Chunker getChunker(String user, PlaybackStateDTO stateDTO) {

        AudioFile audioFile = roomContext.getCurrentAudio();

        Fmp4Chunker chunker = userChunkers.computeIfAbsent(
                user,
                ignored -> {
                    Fmp4Chunker newChunker = new Fmp4Chunker(
                        audioStorageService,
                        audioFile.getPath(),
                        audioFile.getChunks()
                    );
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

        if (currentState == null) return;

        log.info("user connected and recive {}", currentState.position());
        roomWebSocketService.broadcastPlaybackState(user, currentState);
        roomWebSocketService.broadcastAudioInfo(user, AudioDTO.ofAudioFile(roomContext.getCurrentAudio()));
    }

    public void cancelTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            task = null;
        }
    }
}