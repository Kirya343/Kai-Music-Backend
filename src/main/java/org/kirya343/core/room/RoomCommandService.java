package org.kirya343.core.room;

import org.kirya343.core.audio.playback.RoomWebSocketService;
import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.room.RoomDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class RoomCommandService {

    private final ListeningRoomRepository listeningRoomRepository;
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

        roomWebSocketService.broadcastRoomInfo(authData.openId(), RoomDTO.Get.ofRoom(saved));

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
