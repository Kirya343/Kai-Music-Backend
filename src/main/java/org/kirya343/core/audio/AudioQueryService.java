package org.kirya343.core.audio;

import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
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

        return RoomDTO.Get.ofRoom(room);
    }
}
