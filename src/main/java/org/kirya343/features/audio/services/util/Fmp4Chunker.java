package org.kirya343.features.audio.services.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.kirya343.features.audio.dto.AudioChunk;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public class Fmp4Chunker {

    private final List<AudioChunk> chunks;
    private final List<Double> startTimes;
    private AudioChunk initializationChunk;

    private int index = 0;

    public Fmp4Chunker(Path file) throws IOException {
        Fmp4Parser parser = new Fmp4Parser();

        parser.parse(
            Fmp4Encoder.encode(file)
        );

        this.chunks = parser.getAudioChunks();
        this.startTimes = parser.getStartTimes();
        this.initializationChunk = parser.getInitializationChunk();
    }

    public AudioChunk nextAudioChunk() {
        if (index >= chunks.size()) {
            return null;
        }

        return chunks.get(index++);
    }

    public AudioChunk initializationChunk() {
        return initializationChunk;
    }

    public boolean hasNext() {
        return index < chunks.size();
    }

    public void seek(double positionSeconds) {
        int targetIndex = 0;

        for (int i = 0; i < startTimes.size(); i++) {
            if (startTimes.get(i) <= positionSeconds) {
                targetIndex = i;
            } else {
                break;
            }
        }

        index = targetIndex;
    }
}