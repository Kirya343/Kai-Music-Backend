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

import org.kirya343.features.audio.services.cache.RoomPlaybackStateStore;
import org.kirya343.features.audio.services.util.Fmp4Chunker;
import org.kirya343.features.audio.datasource.model.AudioFile;
import org.kirya343.features.audio.datasource.repository.AudioFileRepository;
import org.kirya343.features.audio.dto.AudioChunk;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AudioStreamWorker {

    private final AudioFileRepository audioFileRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomPlaybackStateStore roomPlaybackStateStore;
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    private final Long roomId;

    private AudioFile audioFile;
    private Fmp4Chunker chunker;
    private Set<String> initializedListeners = new HashSet<>();
    private ScheduledFuture<?> task;

    public AudioStreamWorker(
        Long roomId,
        RoomPlaybackStateStore roomPlaybackStateStore,
        AudioFileRepository audioFileRepository,
        SimpMessagingTemplate messagingTemplate
    ) {

        this.roomId = roomId;
        this.roomPlaybackStateStore = roomPlaybackStateStore;
        this.audioFileRepository = audioFileRepository;
        this.messagingTemplate = messagingTemplate;

        log.info(
            "Created AudioStreamWorker for room {}",
            roomId
        );
    }

    public void switchTrack(PlaybackStateDTO stateDTO) {

        log.info(
            "SWITCH TRACK START: room={}, queueEntryId={}",
            roomId,
            stateDTO.entryId()
        );

        stop();

        start(stateDTO);

        log.info(
            "SWITCH TRACK END: room={}",
            roomId
        );
    }

    public void start(PlaybackStateDTO stateDTO) {

        log.info(
            "START STATE: entryId={}, position={}",
            stateDTO.entryId(),
            stateDTO.position()
        );

        loadAudio(stateDTO.entryId());

        chunker.seek(stateDTO.position());

        log.info(
            "START: room={}, audio={}, chunker={}",
            roomId,
            audioFile.getName(),
            chunker
        );

        if (chunker == null) {
            throw new IllegalStateException(
                "Cannot start audio stream without audio"
            );
        }

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
                    sendNextChunk();

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

    public void stop() {

        if (task != null) {

            task.cancel(false);
            task = null;

        }
    }

    private void sendNextChunk() {

        Set<String> listeners = roomPlaybackStateStore.get(roomId).getListeners();

        // удаляем из списка инициализции вышедших пользователей
        initializedListeners.retainAll(listeners);

        Set<String> newListeners = new HashSet<>(listeners);

        // Получаем новых пользователей в комнате
        newListeners.removeAll(initializedListeners);

        // Отправка всем новым пользователям чанка инициализации
        for (String user : newListeners) {
            sendChunk(user, chunker.initializationChunk());
            initializedListeners.add(user);
        }

        if (initializedListeners.isEmpty()) {
            return;
        }

        AudioChunk chunk = chunker.nextAudioChunk();

        if (chunk == null) {
            stop();
            return;
        }

        // Отправка музыки всем инициализированным пользователям
        for (String user : initializedListeners) {
            sendChunk(user, chunk);
        }
    }

    private void sendChunk(String user, AudioChunk chunk) {
        Map<String, Object> headers = new HashMap<>();

        headers.put("content-type", "audio/mp4");
        headers.put("sequence", chunk.sequence());
        headers.put("duration", chunk.durationMs());
        headers.put("initialization", chunk.initialization());

        headers.put("audioId", audioFile.getId());
        headers.put("audioName", audioFile.getName());
        headers.put("audioFormat", audioFile.getFormat());
        headers.put("audioTitle", audioFile.getTitle());
        headers.put("audioArtist", audioFile.getArtist());
        headers.put("audioAlbum", audioFile.getAlbum());
        headers.put("audioDuration", audioFile.getDuration());
        headers.put("audioCoverUrl", audioFile.getCoverUrl());


        messagingTemplate.convertAndSendToUser(
            user,
            "/queue/audio",
            chunk.data(),
            headers
        );
    }

    private void loadAudio(Long queueItemId) {

        this.audioFile = audioFileRepository
            .findAudioByQueueItem(queueItemId)
            .orElseThrow();

        log.info(
            "NEW AUDIO: id={}, name={}, path={}",
            audioFile.getId(),
            audioFile.getName(),
            audioFile.getPath()
        );

        loadChunker(this.audioFile);
    }

    private void loadChunker(AudioFile audioFile) {

        log.info(
            "Loading fMP4 chunker: {}",
            audioFile.getPath()
        );

        Path path = Path.of(
            audioFile.getPath()
        );

        try {

            this.chunker = new Fmp4Chunker(path);

        } catch (IOException e) {

            throw new RuntimeException(
                "Failed to create Fmp4Chunker for audio: "
                    + audioFile.getPath(),
                e
            );
        }
    }
}