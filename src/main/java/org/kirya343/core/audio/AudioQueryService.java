package org.kirya343.core.audio;

import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.dto.audio.RoomDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioQueryService {

    private final ListeningRoomRepository listeningRoomRepository;
    private final AudioMappingService audioMappingService;
    
    public RoomDTO.Get getCurrentRoom(UserAuthData authData) {

        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id())
            .orElseThrow(() -> new EntityNotFoundException("Комната не найдена"));

        return new RoomDTO.Get(
            room.getId(),
            room.getTitle() != null ? room.getTitle() : room.getOwner().getName() + "\'s room",
            room.getOwner().getId(),
            room.getMembers().size(),
            room.getPlaybackMode(),
            room.getQueue().stream().map(qi -> audioMappingService.toDTO(qi)).toList()
        );
    }
}
