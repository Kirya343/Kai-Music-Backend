package org.kirya343.features.audio.services.streaming;

import java.io.IOException;
import java.nio.file.Path;
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
import org.kirya343.features.audio.services.util.AudioMp3Service;
import org.kirya343.features.audio.services.util.Fmp4Chunker;
import org.kirya343.features.audio.services.util.Fmp4Parser;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.dto.RoomDTO;
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
    private Fmp4Parser parser;
    private long playbackPosition = 0;
    private Set<String> initializedListeners = new HashSet<>();
    private ScheduledFuture<?> task;
    private final Map<String, Fmp4Chunker> userChunkers = new HashMap<>();

    private final Map<String, Double> bufferedUntil = new HashMap<>();
    private static final int PAUSED_BUFFER_SECONDS = 15;

    public AudioStreamWorker(
        Long roomId,
        RoomPlaybackContextStore roomPlaybackContextStore,
        SimpMessagingTemplate messagingTemplate,
        ApplicationEventPublisher eventPublisher,
        RoomWebSocketService roomWebSocketService,
        AudioStorageService audioStorageService
    ) {

        this.roomId = roomId;
        this.roomPlaybackContextStore = roomPlaybackContextStore;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.roomWebSocketService = roomWebSocketService;
        this.audioStorageService = audioStorageService;

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

    public void deleteInitializedListener(String user) {
        this.initializedListeners.remove(user);
    }

    public void start(PlaybackStateDTO stateDTO) {

        boolean trackChanged =
            currentState != null &&
            !currentState.entryId().equals(stateDTO.entryId());

        /**
         * if track is changed - clear all users and user chunkers,
         * becouse for new track audioStreamWorker have to create new parser with new track
         * and recreate all user chunkers with new parser
         */
        if (trackChanged) {
            log.info(
                "SWITCH TRACK: {} => {}",
                currentState.entryId(),
                stateDTO.entryId()
            );

            stop(stateDTO);
            initializedListeners.clear();
            userChunkers.clear();
        }

        log.info("START STATE: entryId={}, position={}", stateDTO.entryId(), stateDTO.position());

        updateState(stateDTO);

        if (parser == null) return;

        initializedListeners.forEach(u -> getChunker(u, stateDTO).seek(stateDTO.position()));

        long duration = AudioMp3Service.getDuration(getAudioFile());

        log.info("audioDuration: {}, pos: {}", duration, stateDTO.position());

        playbackPosition = stateDTO.position();

        log.info(
            "START: room={}, audio={}, chunker={}",
            roomId,
            getAudioFile().getName(),
            parser
        );

        if (task != null && !task.isDone() && !task.isCancelled()) {

            log.warn(
                "Worker already running: room={}",
                roomId
            );

            return;
        }

        task = scheduler.scheduleAtFixedRate(
            () -> {

                try {

                    streamTick(stateDTO);

                } catch (Throwable e) {

                    log.error("SCHEDULED TASK CRASHED: room={}", roomId, e);
                }

            },
            0,
            CHUNK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    public void streamTick(PlaybackStateDTO stateDTO) throws IOException {
        Set<String> listeners = roomPlaybackContextStore.computeIfAbsent(roomId).getListeners();

        //log.info("listeners: {}", String.join(",", listeners));
        //log.info("initializedListeners: {}", String.join(",", initializedListeners));

        if (playbackPosition > getAudioFile().getDuration()) {

            log.debug("Sending NEXT event: room={}", roomId);
            eventPublisher.publishEvent(new Next(roomId, UserAuthData.server()));
            stop(stateDTO);
            return;
        }

        for (String user : listeners) {

            Fmp4Chunker chunker = getChunker(user, stateDTO);

            if (!initializedListeners.contains(user)) {
                sendChunk(user, chunker.initializationChunk());
                initializedListeners.add(user);
            }

            if (chunker.hasNext()) {

                AudioChunk chunk = chunker.nextAudioChunk();

                if (currentState.pause()) {
                    double target = currentState.position() + PAUSED_BUFFER_SECONDS;

                    if (bufferedUntil.getOrDefault(user, 0.0) < target) {

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
                    sendChunk(user, chunk);
                }
            }
        }

        if (!currentState.pause()) {
            playbackPosition += 3;
        }
    }

    public void stop(PlaybackStateDTO stateDTO) {

        if (task != null) {

            task.cancel(false);
            task = null;

        }

        updateState(stateDTO);
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

        if (state.entryId() == null) return;

        boolean audioChanged =
            currentState != null &&
            !currentState.entryId().equals(state.entryId());

        /**
         * if audiofile in room is null, or if playback.entryId is changed, then:
         *  - get audio by queue entry id and set it to RoomPlaybackContext
         *  - updating currentState if audio is changed
         */
        if (
            getAudioFile() == null ||
            currentState == null ||
            audioChanged
        ) {
            if (audioChanged) {
                initializedListeners.clear();
                userChunkers.clear();
                playbackPosition = 0;
            }

            currentState = state;

            roomPlaybackContextStore.updateRoomAudio(roomId, state);
            loadParser();

            RoomPlaybackContext context =
                roomPlaybackContextStore.get(roomId);

            try {
                for (String user : context.getListeners()) {
                    getChunker(user, currentState);

                    roomWebSocketService.broadcastRoomInfo(
                        user,
                        RoomDTO.ofRoomPlaybackContext(context)
                    );
                }
            } catch (Exception e) {
                log.info("Exception {}", e);
            }
        }
    }

    private void loadParser() {

        log.info(
            "Loading fMP4 chunker: {}",
            getAudioFile().getPath()
        );

        Path path = Path.of(
            getAudioFile().getPath()
        );

        try {

            this.parser = new Fmp4Parser(path);

        } catch (Exception e) {

            throw new RuntimeException(
                "Failed to create Fmp4Chunker for audio: "
                    + getAudioFile().getPath(),
                e
            );
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
}