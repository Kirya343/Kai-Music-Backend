package org.kirya343.core.audio;

import org.kirya343.datasource.model.audio.AudioFile;
import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.model.audio.QueueItem;
import org.kirya343.dto.audio.AudioDTO;
import org.kirya343.dto.audio.QueueItemDTO;
import org.kirya343.dto.audio.ShortListeningRoomDTO;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioMappingService {
    
    public AudioDTO toDTO(AudioFile audio) {
        return new AudioDTO(audio.getId(), audio.getName());
    }

    public QueueItemDTO toDTO(QueueItem qi) {
        return new QueueItemDTO(
            qi.getId(), 
            qi.getAudio().getId(),
            qi.getAudio().getName(), 
            qi.getPosition()
        );
    }

    public ShortListeningRoomDTO toShortDTO(ListeningRoom room) {
        return new ShortListeningRoomDTO(
            room.getId(), 
            room.getOwner().getName() + "\'s room", 
            room.getOwner().getId(),
            room.getMembers().size()
        );
    }
}
