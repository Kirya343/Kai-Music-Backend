package org.kirya343.dto.audio;

public record AudioChunk(
    byte[] data,
    long sequence,
    long durationMs,
    boolean initialization
) {
}
