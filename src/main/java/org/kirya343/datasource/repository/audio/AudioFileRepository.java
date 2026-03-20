package org.kirya343.datasource.repository.audio;

import java.util.List;
import java.util.Optional;

import org.kirya343.datasource.model.audio.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {
    
    @Query("""
        SELECT a
        FROM QueueItem q
        JOIN q.audio a
        JOIN q.room r
        LEFT JOIN r.members m
        WHERE q.id = :queueItemId
        AND (r.owner.id = :userId OR m.id = :userId)
    """)
    Optional<AudioFile> findAudioInUserRoom(
        Long userId,
        Long queueItemId
    );

    List<AudioFile> findByOwnerId(Long ownerId);
}
