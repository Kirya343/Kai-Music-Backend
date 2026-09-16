package org.kirya343.features.audio.services;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.dto.RoomDTO;
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
