package org.kirya343.dto.audio;

public record AudioDTO(
    Long id,
    String name,
    String format,
    String title,
    String artist,
    String album,
    Long duration,
    String coverUrl
) {
}
