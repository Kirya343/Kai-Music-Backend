package org.kirya343.features.playlist.dto;

import java.util.List;

import org.kirya343.features.playlist.dto.queue.QueueItemCreateDTO;

public record PlaylistCreateDTO(
    String title,
    List<QueueItemCreateDTO> audios
) {
}
