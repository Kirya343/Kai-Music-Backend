package org.kirya343.features.room.services;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.user.datasource.User;
import org.kirya343.features.user.datasource.UserRepository;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.services.RoomWebSocketService;
import org.kirya343.features.room.dto.RoomDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class RoomCommandService {

    private final ListeningRoomRepository listeningRoomRepository;
    private final UserRepository userRepository;
    private final RoomWebSocketService roomWebSocketService;
    private final EntityManager entityManager;
    
    @Transactional 
    public ListeningRoom createRoom(UserAuthData authData) {
        listeningRoomRepository.findAllByOwnerId(authData.id())
            .forEach(r -> deleteRoom(r));

        ListeningRoom room = new ListeningRoom(
            entityManager.getReference(User.class, authData.id())
        );

        ListeningRoom saved = listeningRoomRepository.save(room);
        userRepository.updateListeningRoom(authData.id(), saved.getId());

        roomWebSocketService.broadcastRoomInfo(authData.openId(), RoomDTO.ofRoom(saved));

        return saved;
    }

    @Transactional
    public void deleteRoom(ListeningRoom room) {
        for (User member : room.getMembers()) {
            member.setListeningRoom(null);
        }

        listeningRoomRepository.delete(room);
    }
}
