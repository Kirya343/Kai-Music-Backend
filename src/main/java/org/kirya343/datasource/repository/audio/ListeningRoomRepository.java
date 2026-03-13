package org.kirya343.datasource.repository.audio;

import org.kirya343.datasource.model.user.audio.ListeningRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListeningRoomRepository extends JpaRepository<ListeningRoom, Long> {
    
}
