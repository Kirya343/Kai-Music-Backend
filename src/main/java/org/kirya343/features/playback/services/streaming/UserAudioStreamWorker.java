package org.kirya343.features.playback.services.streaming;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.audio.dto.AudioChunk;
import org.kirya343.features.audio.services.AudioQueryService;
import org.kirya343.features.audio.services.storage.AudioStorageService;
import org.kirya343.features.audio.services.util.Fmp4Chunker;
import org.kirya343.features.playback.PlaybackWebsocketService;
import org.kirya343.features.playback.dto.PlaybackStateDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public class UserAudioStreamWorker {

    private final String user;
    private boolean isInitialized;
    private Double bufferedUntil = 0.0;
    private PlaybackStateDTO currentState;

    private static final int BUFFER_SECONDS = 20;

    private ScheduledFuture<?> task;

    private Fmp4Chunker chunker;

    private final AudioStorageService audioStorageService;
    private final PlaybackWebsocketService playbackWebsocketService;
    private final AudioQueryService audioQueryService;

    public UserAudioStreamWorker(
        String user,
        AudioQueryService audioQueryService,
        AudioStorageService audioStorageService,
        PlaybackWebsocketService playbackWebsocketService
    ) {
        this.user = user;
        this.audioQueryService = audioQueryService;
        this.audioStorageService = audioStorageService;
        this.playbackWebsocketService = playbackWebsocketService;
    }

    public void applyState(PlaybackStateDTO state) {

        log.info("applyState state={}", state == null);

        if (state == null) return;

        updateState(state);

        log.info("START STATE: entryId={}, position={}", state.entryId(), state.position());

    }

    public void tick() throws IOException {

        log.debug("Tick to user {}", user);

        if (!isInitialized) {
            initializeUser();
        }

        if (chunker.hasNext()) {
            double target = currentState.position() + BUFFER_SECONDS;

            if (bufferedUntil < target) {

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

                bufferedUntil = chunkDurationUntil;
            }
        }
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

            isInitialized = false;
            bufferedUntil = 0.0;
            reloadChunker(currentState);
        }

        if (positionChanged && !trackChanged) {
            log.info(
                "SEEK POSITION: {} => {}",
                previousState != null ? previousState.position() : "null",
                currentState.position()
            );

            chunker.seek(currentState.position());
            bufferedUntil = currentState.position();
        }
    }

    private void initializeUser() throws IOException {

        playbackWebsocketService.sendChunk(
            user, 
            chunker.initializationChunk(), 
            currentState.entryId());

        bufferedUntil = currentState.position();

        isInitialized = true;
    }

    private void reloadChunker(PlaybackStateDTO stateDTO) {

        AudioFile audioFile = audioQueryService.getAudioByQueueItem(stateDTO.entryId());

        chunker = new Fmp4Chunker(
            audioStorageService,
            audioFile.getPath(),
            audioFile.getChunks()
        );
        log.debug("Перематываем чанкер для пользователя {} на позицию: {}", user, stateDTO.position());

        chunker.seek(stateDTO.position());
    }

    public void cancelTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            task = null;
        }
    }
}