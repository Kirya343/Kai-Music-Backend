package org.kirya343.core.audio.streaming;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kirya343.core.audio.util.Mp3Chunker;
import org.kirya343.datasource.model.audio.AudioFile;
import org.kirya343.datasource.repository.audio.AudioFileRepository;
import org.kirya343.dto.audio.AudioChunk;
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
    private Mp3Chunker chunker;
    private ScheduledFuture<?> task;

    public AudioStreamWorker(
            Long roomId,
            AudioFileRepository audioFileRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.roomId = roomId;
        this.audioFileRepository = audioFileRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void switchTrack(Long audioId) {

        stop();

        this.audioFile = audioFileRepository
                .findById(audioId)
                .orElseThrow();

        try {
            this.chunker = new Mp3Chunker(
                    Path.of(audioFile.getPath())
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load audio file: " + audioFile.getPath(),
                    e
            );
        }

        start();
    }

    public void start() {

        if (chunker == null) {
            throw new IllegalStateException(
                    "Cannot start audio stream without audio"
            );
        }

        task = scheduler.scheduleAtFixedRate(
                this::sendNextChunk,
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

        AudioChunk chunk = chunker.nextChunk();

        if (chunk == null) {
            stop();
            return;
        }

        log.info(
                "Sending audio chunk: {} bytes",
                chunk.data().length
        );

        messagingTemplate.convertAndSend(
            "/topic/room/" + roomId + "/audio",
            chunk.data(),
            Map.of(
                "content-type", "audio/mpeg",
                "sequence", chunk.sequence(),
                "offset", chunk.offset(),
                "duration", chunk.duration()
            )
        );
    }
}
