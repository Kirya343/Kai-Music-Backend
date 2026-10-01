package org.kirya343.features.playlist.datasource.repository;

import java.util.List;
import java.util.Optional;

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

    @Query("""
        SELECT r.playlist
        FROM ListeningRoom r
        WHERE r.id = :roomId
    """)
    Optional<Playlist> findByRoomId(@Param("roomId") Long roomId);

    @Query("""
        SELECT p
        FROM Playlist p
        WHERE p.owner.id = :userId
        AND NOT EXISTS (
            SELECT r
            FROM ListeningRoom r
            WHERE r.playlist = p
        )
    """)
    List<Playlist> findUserPlaylists(@Param("userId") Long userId);
}
