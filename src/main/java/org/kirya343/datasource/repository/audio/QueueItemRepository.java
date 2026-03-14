package org.kirya343.datasource.repository.audio;

import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.kirya343.datasource.model.user.audio.ListeningRoom;
import org.kirya343.datasource.model.user.audio.QueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface QueueItemRepository extends JpaRepository<QueueItem, Long> {
 
    Optional<QueueItem> findFirstByRoomOrderByPositionAsc(ListeningRoom room);

    @Query("SELECT COALESCE(MAX(q.position), 0) FROM QueueItem q")
    Long findMaxPosition();
}
