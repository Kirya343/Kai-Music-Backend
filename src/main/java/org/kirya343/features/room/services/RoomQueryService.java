package org.kirya343.features.room.services;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.room.dto.RoomDTO;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class RoomQueryService {

    private final ListeningRoomRepository listeningRoomRepository;
    
    public RoomDTO getCurrentRoom(UserAuthData authData) {
        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id()).orElseThrow();

        return RoomDTO.ofRoom(room);
    }

    public ListeningRoom findByPlaylistId(Long playlistId) {
        return listeningRoomRepository.findByPlaylistId(playlistId).orElse(null);
    }
}
