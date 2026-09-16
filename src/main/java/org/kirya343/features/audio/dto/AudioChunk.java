package org.kirya343.features.audio.dto;

public record AudioChunk(
    byte[] data,
    long sequence,
    long durationMs,
    boolean initialization
) {
}
