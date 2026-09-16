package org.kirya343.core.audio.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.kirya343.dto.audio.AudioChunk;

import lombok.extern.slf4j.Slf4j;

@Slf4j 
public class Fmp4Chunker {

    private final List<AudioChunk> chunks;
    private final List<Double> startTimes;
    private boolean initializationPending = false;
    private AudioChunk initializationChunk;

    private int index = 0;

    public Fmp4Chunker(Path file) throws IOException {
        Fmp4Parser parser = new Fmp4Parser();

        Fmp4Parser.Result result = parser.parse(
            Fmp4Encoder.encode(file)
        );

        this.chunks = result.chunks();
        this.startTimes = result.startTimes();
        this.initializationChunk = result.initializationChunk();
    }

    public AudioChunk nextChunk() {
        if (initializationPending) {
            initializationPending = false;
            return initializationChunk;
        }

        if (index >= chunks.size()) {
            return null;
        }

        return chunks.get(index++);
    }

    public boolean hasNext() {
        return initializationPending || index < chunks.size();
    }

    public void seek(double positionSeconds) {
        int targetIndex = 1;

        for (int i = 0; i < startTimes.size(); i++) {
            if (startTimes.get(i) <= positionSeconds) {
                targetIndex = i + 1;
            } else {
                break;
            }
        }

        index = targetIndex;
        initializationPending = true;
    }
}