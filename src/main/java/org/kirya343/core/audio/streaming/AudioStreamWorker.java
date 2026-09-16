package org.kirya343.core.audio.streaming;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kirya343.core.audio.util.Fmp4Chunker;
import org.kirya343.datasource.model.audio.AudioFile;
import org.kirya343.datasource.repository.audio.AudioFileRepository;
import org.kirya343.dto.audio.AudioChunk;
import org.kirya343.dto.audio.PlaybackStateDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AudioStreamWorker {

    private final AudioFileRepository audioFileRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    private final Long roomId;

    private AudioFile audioFile;
    private Fmp4Chunker chunker;
    private ScheduledFuture<?> task;

    public AudioStreamWorker(
        Long roomId,
        AudioFileRepository audioFileRepository,
        SimpMessagingTemplate messagingTemplate
    ) {

        this.roomId = roomId;
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

        try {

            AudioChunk chunk = chunker.nextChunk();

            if (chunk == null) {

                log.info(
                    "Audio stream finished: room={}, audio={}",
                    roomId,
                    audioFile.getName()
                );

                stop();

                return;
            }

            log.info(
                "Sending audio chunk: room={}, audio={}, sequence={}, bytes={}, initialization={}",
                roomId,
                audioFile.getName(),
                chunk.sequence(),
                chunk.data().length,
                chunk.initialization()
            );

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

            messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/audio",
                chunk.data(),
                headers
            );

        } catch (Exception e) {

            log.error(
                "WORKER FAILED: room={}, audio={}",
                roomId,
                audioFile.getName(),
                e
            );
        }
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