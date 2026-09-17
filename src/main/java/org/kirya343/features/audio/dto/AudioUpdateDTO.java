package org.kirya343.features.audio.dto;

public record AudioUpdateDTO(
    String title,
    String artist,
    String album,
    String coverUrl
) {}
