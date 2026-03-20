package org.kirya343.datasource.repository.audio;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.kirya343.datasource.model.audio.AudioFile;
import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.model.audio.QueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface QueueItemRepository extends JpaRepository<QueueItem, Long> {
 
    Optional<QueueItem> findFirstByRoomOrderByPositionAsc(ListeningRoom room);
    List<QueueItem> findByRoomIdOrderByPosition(Long roomId);

    Optional<QueueItem> findByRoomIdAndAudioId(Long roomId, Long audioId);

    @Query("SELECT COALESCE(MAX(q.position), 0) FROM QueueItem q")
    Long findMaxPosition();

    @Query("""
        SELECT q.audio
        FROM QueueItem q
        WHERE q.room.id = :roomId
        AND q.id = :queueEntryId
    """)
    Optional<AudioFile> findAudioInRoomQueue(Long roomId, Long queueEntryId);

    @Query("SELECT q.audio FROM QueueItem q WHERE q.id = :queueItemId")
    AudioFile findAudioById(@Param("queueItemId") Long queueItemId);

    @Query(value = """
        SELECT * FROM queue_items q
        WHERE q.room_id = :roomId
        AND q.position > (
            SELECT q2.position FROM queue_items q2
            WHERE q2.room_id = :roomId AND q2.audio_id = :audioId
            ORDER BY q2.position ASC
            LIMIT 1
        )
        ORDER BY q.position ASC
        LIMIT 1
    """, nativeQuery = true)
    Optional<QueueItem> findNextTrack(@Param("roomId") Long roomId, @Param("audioId") Long audioId);

    @Query("SELECT q FROM QueueItem q " +
           "WHERE q.room.id = :roomId " +
           "ORDER BY q.position ASC")
    Optional<QueueItem> findFirstByRoomIdOrderByPositionAsc(@Param("roomId") Long roomId);

    @Query(value = """
        SELECT * FROM queue_items
        WHERE room_id = :roomId
        ORDER BY RAND()
        LIMIT 1
    """, nativeQuery = true)
    Optional<QueueItem> findRandomTrack(Long roomId);
}
