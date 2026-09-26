package org.kirya343.features.playlist.dto;

import java.util.List;

import org.kirya343.features.playback.enums.PlaybackMode;
import org.kirya343.features.playlist.datasource.model.Playlist;
import org.kirya343.features.playlist.dto.queue.QueueItemDTO;

public record PlaylistDTO(
    Long id,
    Long ownerId,
    String title,
    PlaybackMode mode,
    List<QueueItemDTO> queue
) {
    public static PlaylistDTO ofPlaylist(Playlist playlist) {

        return new PlaylistDTO(
            playlist.getId(),
            playlist.getOwner().getId(),
            playlist.getTitle(),
            playlist.getPlaybackMode(),
            QueueItemDTO.ofList(playlist.getQueue())
        );
    }
}
