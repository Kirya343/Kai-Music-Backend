package org.kirya343.core.audio;

import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.dto.audio.ListeningRoomDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioQueryService {

    private final ListeningRoomRepository listeningRoomRepository;
    private final AudioMappingService audioMappingService;
    
    public ListeningRoomDTO getCurrentRoom(UserAuthData authData) {

        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id())
            .orElseThrow(() -> new EntityNotFoundException("Комната не найдена"));

        return new ListeningRoomDTO(
            room.getId(),
            room.getOwner().getName() + "\'s room", 
            room.getOwner().getId(),
            room.getMembers().size(),
            room.getPlaybackMode(),
            room.getQueue().stream().map(qi -> audioMappingService.toDTO(qi)).toList()
        );
    }
}
