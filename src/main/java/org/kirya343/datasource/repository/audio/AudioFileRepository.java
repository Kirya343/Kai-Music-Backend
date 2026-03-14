package org.kirya343.datasource.repository.audio;

import java.util.List;
import java.util.Optional;

import org.kirya343.datasource.model.user.audio.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {
    
    @Query("""
        SELECT a
        FROM QueueItem q
        JOIN q.audio a
        JOIN q.room r
        LEFT JOIN r.members m
        WHERE a.id = :audioId
        AND (r.owner.id = :userId OR m.id = :userId)
    """)
    Optional<AudioFile> findAudioInUserRoom(
        @Param("userId") Long userId,
        @Param("audioId") Long audioId
    );

    List<AudioFile> findByOwnerId(Long ownerId);
}
