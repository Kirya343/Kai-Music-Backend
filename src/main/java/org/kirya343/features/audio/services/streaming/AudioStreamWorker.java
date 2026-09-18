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
import org.kirya343.features.audio.services.util.AudioMp3Service;
import org.kirya343.features.audio.services.util.Fmp4Chunker;
import org.kirya343.features.audio.services.util.Fmp4Parser;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.dto.RoomDTO;
import org.kirya343.features.room.dto.commands.Next;
import org.kirya343.features.audio.datasource.model.AudioFile;
import org.kirya343.features.audio.datasource.repository.AudioFileRepository;
import org.kirya343.features.audio.dto.AudioChunk;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AudioStreamWorker {

    private final AudioFileRepository audioFileRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();
    private final ApplicationEventPublisher eventPublisher;
    private final RoomWebSocketService roomWebSocketService;

    private final Long roomId;

    private PlaybackStateDTO currentState;
    private Fmp4Parser parser;
    private long audioTimeLeft;
    private Set<String> initializedListeners = new HashSet<>();
    private ScheduledFuture<?> task;
    private final Map<String, Fmp4Chunker> userChunkers = new HashMap<>();

    public AudioStreamWorker(
        Long roomId,
        RoomPlaybackContextStore roomPlaybackContextStore,
        AudioFileRepository audioFileRepository,
        SimpMessagingTemplate messagingTemplate,
        ApplicationEventPublisher eventPublisher,
        RoomWebSocketService roomWebSocketService
    ) {

        this.roomId = roomId;
        this.roomPlaybackContextStore = roomPlaybackContextStore;
        this.audioFileRepository = audioFileRepository;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.roomWebSocketService = roomWebSocketService;

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

    public void switchTrack(PlaybackStateDTO stateDTO) {

        log.info(
            "SWITCH TRACK START: room={}, queueEntryId={}",
            roomId,
            stateDTO.entryId()
        );

        initializedListeners.clear();

        stop(stateDTO);

        start(stateDTO);

        log.info(
            "SWITCH TRACK END: room={}",
            roomId
        );
    }

    public void updateState(PlaybackStateDTO stateDTO) {
        loadAudio(stateDTO);
    }

    public void start(PlaybackStateDTO stateDTO) {

        log.info(
            "START STATE: entryId={}, position={}",
            stateDTO.entryId(),
            stateDTO.position()
        );

        updateState(stateDTO);
        if (parser == null) return;

        initializedListeners.forEach(u -> getChunker(u, stateDTO).seek(stateDTO.position()));

        long duration = AudioMp3Service.getDuration(getAudioFile());

        log.info("audioDuration: {}, pos: {}", duration, stateDTO.position());

        audioTimeLeft = duration - stateDTO.position();

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

                    Set<String> listeners = roomPlaybackContextStore.computeIfAbsent(roomId).getListeners();

                    //log.info("listeners: {}", String.join(",", listeners));
                    //log.info("initializedListeners: {}", String.join(",", initializedListeners));
                    //log.info("audioTimeLeft: {}", audioTimeLeft);

                    if (audioTimeLeft < 0) {

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

                        AudioChunk chunk = chunker.nextAudioChunk();

                        if (chunk != null) {
                            sendChunk(user, chunk);
                        }
                    }

                    audioTimeLeft = audioTimeLeft - 3;

                } catch (Throwable e) {

                    log.error(
                        "SCHEDULED TASK CRASHED: room={}",
                        roomId,
                        e
                    );
                }

            },
            0,
            3,
            TimeUnit.SECONDS
        );
    }

    public void stop(PlaybackStateDTO stateDTO) {

        loadAudio(stateDTO);

        if (task != null) {

            task.cancel(false);
            task = null;

        }
    }

    private void sendChunk(String user, AudioChunk chunk) {
        Map<String, Object> headers = new HashMap<>();

        log.debug("Sending chunk to {}: chunk={}", user, chunk.sequence());

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

    private void loadAudio(PlaybackStateDTO state) {

        if (state.entryId() == null) return;

        if (
            getAudioFile() == null || 
            currentState == null ||
            !currentState.entryId().equals(state.entryId())
        ) {
            AudioFile audioFile = audioFileRepository
                .findAudioByQueueItem(state.entryId())
                .orElseThrow();

            roomPlaybackContextStore.get(roomId).setCurrentAudio(audioFile);

            RoomPlaybackContext context = roomPlaybackContextStore.get(this.roomId);

            this.currentState = state;

            log.info(
                "NEW AUDIO: id={}, name={}, path={}",
                getAudioFile().getId(),
                getAudioFile().getName(),
                getAudioFile().getPath()
            );

            loadParser();

            try {
                for (String user : context.getListeners()) {
                    getChunker(user, state);

                    roomWebSocketService.broadcastRoomInfo(user, RoomDTO.ofRoomPlaybackContext(context));
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
                        newChunker = new Fmp4Chunker(parser);
                    } catch (IOException e) {
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