package org.kirya343.features.audio.services.util;

import java.io.IOException;

import org.kirya343.features.audio.dto.AudioChunk;
import org.kirya343.features.audio.services.storage.AudioStorageService;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public class Fmp4Chunker {

    private final AudioStorageService audioStorageService;
    private final int CHUNK_DURATION_SECONDS = 5;

    private final String audioDirectory;

    private final int chunkCount;

    private int index = 0;

    public Fmp4Chunker(
        AudioStorageService audioStorageService,
        String audioDirectory,
        int chunkCount
    ) {
        this.audioStorageService = audioStorageService;
        this.audioDirectory = audioDirectory;
        this.chunkCount = chunkCount;
    }

    public AudioChunk nextAudioChunk() throws IOException {
        if (!hasNext()) {
            return null;
        }

        int sequence = index++;

        return audioStorageService.getMediaChunk(
            audioDirectory,
            sequence
        );
    }

    public AudioChunk initializationChunk()
        throws IOException {

        return audioStorageService.getInitializationChunk(
            audioDirectory
        );
    }

    public boolean hasNext() {
        return index < chunkCount;
    }

    public void seek(double positionSeconds) {
        log.debug(
            "chunker seek to {}",
            positionSeconds
        );

        if (positionSeconds <= 4) {
            positionSeconds = 0;
        } else {
            positionSeconds -= 3;
        }

        index = (int) Math.floor(
            positionSeconds / CHUNK_DURATION_SECONDS
        );
    }
}