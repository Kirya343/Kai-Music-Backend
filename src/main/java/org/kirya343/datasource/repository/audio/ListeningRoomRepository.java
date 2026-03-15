package org.kirya343.datasource.repository.audio;

import java.util.Optional;

import org.kirya343.datasource.model.user.audio.ListeningRoom;
import org.kirya343.enums.PlaybackMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ListeningRoomRepository extends JpaRepository<ListeningRoom, Long> {
    
    @Query("""
        SELECT r FROM ListeningRoom r
        LEFT JOIN FETCH r.owner
        LEFT JOIN FETCH r.members m
        LEFT JOIN FETCH r.queue q
        LEFT JOIN FETCH q.audio
        WHERE m.id = :userId
    """)
    Optional<ListeningRoom> findRoomByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ListeningRoom r SET r.playbackMode = :mode WHERE r.id = :roomId")
    int updatePlaybackMode(@Param("roomId") Long roomId, @Param("mode") PlaybackMode mode);
}
