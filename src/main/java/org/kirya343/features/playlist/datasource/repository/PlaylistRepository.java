package org.kirya343.features.playlist.datasource.repository;

import org.kirya343.features.playback.enums.PlaybackMode;
import org.kirya343.features.playlist.datasource.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    
    @Modifying
    @Transactional 
    @Query("UPDATE Playlist p SET p.playbackMode = :mode WHERE p.id = :playlistId")
    int updatePlaybackMode(@Param("playlistId") Long playlistId, @Param("mode") PlaybackMode mode);
}
