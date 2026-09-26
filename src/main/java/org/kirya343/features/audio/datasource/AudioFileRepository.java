package org.kirya343.features.audio.datasource;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {
    
    @Query("""
        SELECT a
        FROM QueueItem q
        JOIN q.audio a
        JOIN q.playlist p
        JOIN ListeningRoom r ON r.playlist.id = p.id
        LEFT JOIN r.members m
        WHERE q.id = :queueItemId
        AND (r.owner.id = :userId OR m.id = :userId)
        """)
    Optional<AudioFile> findAudioInUserRoom(
        Long userId,
        Long queueItemId
    );

    @Query("""
        SELECT a
        FROM QueueItem q
        JOIN q.audio a
        WHERE q.id = :queueItemId
    """)
    Optional<AudioFile> findAudioByQueueItem(
        Long queueItemId
    );

    @Query("""
        SELECT a
        FROM QueueItem q
        JOIN q.audio a
        JOIN ListeningRoom r ON r.playlist.id = q.playlist.id
        JOIN r.playbackState ps
        WHERE r.id = :roomId
            AND ps.currentQueueEntryId = q.id
        """)
    Optional<AudioFile> findCurrentAudioByRoom(
        @Param("roomId") Long roomId
    );

    List<AudioFile> findByOwnerId(Long ownerId);
}
