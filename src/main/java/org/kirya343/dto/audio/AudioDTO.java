package org.kirya343.dto.audio;

public class AudioDTO {

    public record Get(
        Long id,
        String name,
        String format,
        String title,
        String artist,
        String album,
        Long duration,
        String coverUrl
    ) {}

    public record Update(
        String title,
        String artist,
        String album,
        String coverUrl
    ) {}
}
