package org.kirya343.features.playback.datasource.repository;

import java.util.List;
import java.util.Optional;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.playback.datasource.model.QueueItem;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QueueItemRepository extends JpaRepository<QueueItem, Long> {
 
    Optional<QueueItem> findFirstByRoomOrderByPositionAsc(ListeningRoom room);
    List<QueueItem> findByRoomIdOrderByPosition(Long roomId);
    void deleteByIdAndRoomId(Long id, Long roomId);

    @Query("""
        SELECT q
        FROM QueueItem q
        JOIN q.room r
        JOIN User u ON u.listeningRoom.id = r.id
        WHERE q.id = :id
        AND u.id = :userId
        """)
    Optional<QueueItem> findByIdAndUserRoom(
        @Param("id") Long id,
        @Param("userId") Long userId
    );

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Query("""
        DELETE FROM QueueItem q
        WHERE q.id = :id
        AND EXISTS (
            SELECT 1
            FROM User u
            WHERE u.id = :userId
                AND u.listeningRoom.id = q.room.id
        )
        """)
    int deleteByIdAndUserRoom(
        @Param("id") Long id,
        @Param("userId") Long userId
    );

    Optional<QueueItem> findByRoomIdAndId(Long roomId, Long entryId);

    @Query("""
        SELECT COALESCE(MAX(q.position), -1)
        FROM QueueItem q
        WHERE q.room.id = :roomId
        """)
    int getMaxPosition(@Param("roomId") Long roomId);

    @Query("""
        SELECT q.audio
        FROM QueueItem q
        WHERE q.room.id = :roomId
        AND q.id = :queueEntryId
    """)
    Optional<AudioFile> findAudioInRoomQueue(Long roomId, Long queueEntryId);

    @Query("SELECT q.audio FROM QueueItem q WHERE q.id = :queueItemId")
    Optional<AudioFile> findAudioById(@Param("queueItemId") Long queueItemId);

    @Query(value = """
        SELECT * FROM queue_items q
        WHERE q.room_id = :roomId
        AND q.position > (
            SELECT q2.position FROM queue_items q2
            WHERE q2.room_id = :roomId AND q2.id = :entryId
            ORDER BY q2.position ASC
            LIMIT 1
        )
        ORDER BY q.position ASC
        LIMIT 1
    """, nativeQuery = true)
    Optional<QueueItem> findNextTrack(Long roomId, Long entryId);

    @Query(value = """
        SELECT * FROM queue_items q
        WHERE q.room_id = :roomId
        AND q.position < (
            SELECT q2.position FROM queue_items q2
            WHERE q2.room_id = :roomId AND q2.id = :entryId
            ORDER BY q2.position ASC
            LIMIT 1
        )
        ORDER BY q.position DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<QueueItem> findPrevTrack(Long roomId, Long entryId);

    Optional<QueueItem> findFirstByRoomIdOrderByPositionAsc(Long roomId);
    Optional<QueueItem> findFirstByRoomIdOrderByPositionDesc(Long roomId);

    @Query(value = """
        SELECT * FROM queue_items
        WHERE room_id = :roomId
        ORDER BY RAND()
        LIMIT 1
    """, nativeQuery = true)
    Optional<QueueItem> findRandomTrack(Long roomId);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE queue_items
        SET position = position + 1
        WHERE room_id = :roomId
        AND position >= :position
        """, nativeQuery = true)
    int shiftQueueItems(
        @Param("roomId") Long roomId,
        @Param("position") Integer position
    );

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Transactional
    @Query(value = """
        INSERT INTO queue_items (
            room_id,
            audio_id,
            position,
            added_by_id,
            created_at
        )
        SELECT
            :roomId,
            :audioId,
            COALESCE(MAX(position), -1) + 1,
            :addedById,
            CURRENT_TIMESTAMP
        FROM queue_items
        WHERE room_id = :roomId
        """, nativeQuery = true)
    int insertQueueItemAtEnd(
        @Param("roomId") Long roomId,
        @Param("audioId") Long audioId,
        @Param("addedById") Long addedById
    );

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Transactional
    @Query(value = """
        INSERT INTO queue_items (
            room_id,
            audio_id,
            position,
            added_by_id,
            created_at
        )
        VALUES (
            :roomId,
            :audioId,
            :position,
            :addedById,
            CURRENT_TIMESTAMP
        )
        """, nativeQuery = true)
    int insertQueueItem(
        @Param("roomId") Long roomId,
        @Param("audioId") Long audioId,
        @Param("position") Integer position,
        @Param("addedById") Long addedById
    );

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Query(value = """
        UPDATE queue_items q
        JOIN (
            SELECT
                id,
                ROW_NUMBER() OVER (
                    PARTITION BY room_id
                    ORDER BY position
                ) - 1 AS new_position
            FROM queue_items
            WHERE room_id = :roomId
        ) ordered ON ordered.id = q.id
        SET q.position = ordered.new_position
        WHERE q.room_id = :roomId
        """, nativeQuery = true)
    int normalizePositions(
        @Param("roomId") Long roomId
    );
}
