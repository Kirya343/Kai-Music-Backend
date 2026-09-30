package org.kirya343.features.playback.services.streaming;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    private PlaybackStateDTO currentState;
    private long playbackStartedAt;

    private ScheduledFuture<?> task;

    private static final double INITIAL_BUFFER_SECONDS = 15;
    private static final double TARGET_BUFFER_SECONDS = 20;
    private static final int CHUNK_INTERVAL_SECONDS = 3;

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
                this::streamTick,
                0,
                CHUNK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            );
        }
    }

    public void streamTick() {

        /**
         * if current playback position is after than audio duration - cancel task and send Next command
         */
        if (getCurrentPosition() > roomContext.getCurrentAudio().getDuration()) {

            log.debug("Sending NEXT event: room={}", roomId);
            eventPublisher.publishEvent(new ChangeTrack(roomId, "next", UserAuthData.server()));
            return;
        }

        try {
            for (String user : roomContext.getListeners()) {
                fillBuffer(user, TARGET_BUFFER_SECONDS);
            }
        } catch (Exception e) {
            log.error(
                "Error while streaming audio for room {}",
                roomId,
                e
            );
        }
    }

    private void fillBuffer(
        String user,
        double targetBufferSeconds
    ) throws IOException {

        Fmp4Chunker chunker = getChunker(user, currentState);

        double buffered = bufferedUntil.getOrDefault(user, 0.0);
        double target = getCurrentPosition() + targetBufferSeconds;

        while (
            buffered < target &&
            chunker.hasNext()
        ) {
            AudioChunk chunk = chunker.nextAudioChunk();

            playbackWebsocketService.sendChunk(
                user,
                chunk,
                currentState.entryId()
            );

            buffered += chunk.durationMs() / 1000.0;
        }

        bufferedUntil.put(user, buffered);
    }

    private void initializeUser(String user) throws IOException {

        Fmp4Chunker chunker = getChunker(user, currentState);

        playbackWebsocketService.sendChunk(
            user, 
            chunker.initializationChunk(), 
            currentState.entryId());

        bufferedUntil.put(
            user,
            currentState.position()
        );

        fillBuffer(user, INITIAL_BUFFER_SECONDS);
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

        playbackStartedAt = System.currentTimeMillis();

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

            userChunkers.clear();
            bufferedUntil.clear();

            try {
                for (String user : roomContext.getListeners()) {
                    roomWebSocketService.broadcastAudioInfo(user, AudioDTO.ofAudioFile(roomContext.getCurrentAudio()));

                    initializeUser(user);
                }
            } catch (Exception e) {
                log.info("Exception {}", e);
            }
        }

        if (positionChanged && !trackChanged) {
            log.info(
                "SEEK POSITION: {} => {}",
                previousState != null ? previousState.position() : "null",
                currentState.position()
            );

            bufferedUntil.clear();

            for (String user : roomContext.getListeners()) {
                Fmp4Chunker chunker = getChunker(user, currentState);

                chunker.seek(currentState.position());
                bufferedUntil.put(user, currentState.position());

                try {
                    fillBuffer(user, INITIAL_BUFFER_SECONDS);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        this.bufferedUntil.remove(user);
        this.userChunkers.remove(user);
    }

    public void handleUserConnected(String user) {

        if (currentState == null) return;

        roomWebSocketService.broadcastPlaybackState(user, currentState);
        roomWebSocketService.broadcastAudioInfo(user, AudioDTO.ofAudioFile(roomContext.getCurrentAudio()));

        try {
            initializeUser(user);
        } catch (IOException e) {
            log.error(
                "Failed to initialize audio for user {}",
                user,
                e
            );
        }
    }

    private double getCurrentPosition() {
        if (currentState.pause()) {
            return currentState.position();
        }

        double elapsed =
            (System.currentTimeMillis() - playbackStartedAt) / 1000.0;

        return currentState.position() + elapsed;
    }

    public void cancelTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            task = null;
        }
    }
}