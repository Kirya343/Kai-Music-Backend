package org.kirya343.features.playback.datasource.repository;

import org.kirya343.features.playback.datasource.model.RoomPlaybackState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPlaybackStateRepository extends JpaRepository<RoomPlaybackState, Long> {
    
}
