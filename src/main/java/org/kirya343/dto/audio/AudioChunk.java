package org.kirya343.dto.audio;

public record AudioChunk(
    byte[] data,
    long sequence,
    long offset,
    long duration
) {}
