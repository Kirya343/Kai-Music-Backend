package org.kirya343.core.audio;

import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.dto.audio.QueueItemDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.room.RoomDTO;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioQueryService {

    private final ListeningRoomRepository listeningRoomRepository;
    
    public RoomDTO.Get getCurrentRoom(UserAuthData authData) {

        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id()).orElseThrow();

        return new RoomDTO.Get(
            room.getId(),
            room.getTitle() != null ? room.getTitle() : room.getOwner().getName() + "\'s room",
            room.getOwner().getId(),
            room.getMembers().size(),
            room.getPlaybackMode(),
            QueueItemDTO.ofList(room.getQueue())
        );
    }
}
