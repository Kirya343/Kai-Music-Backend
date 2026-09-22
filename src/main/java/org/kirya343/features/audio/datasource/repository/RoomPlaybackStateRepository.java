package org.kirya343.features.audio.datasource.repository;

import org.kirya343.features.audio.datasource.model.RoomPlaybackState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPlaybackStateRepository extends JpaRepository<RoomPlaybackState, Long> {
    
}
