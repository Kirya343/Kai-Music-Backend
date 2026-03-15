package org.kirya343.datasource.repository.audio;

import org.kirya343.datasource.model.user.audio.RoomPlaybackState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomPlaybackStateRepository extends JpaRepository<RoomPlaybackState, Long> {

}
