package org.kirya343.datasource.repository.audio;

import org.kirya343.datasource.model.user.audio.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {
    
}
