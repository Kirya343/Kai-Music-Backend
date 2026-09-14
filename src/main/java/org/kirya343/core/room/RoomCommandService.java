package org.kirya343.core.room;

import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class RoomCommandService {

    private final ListeningRoomRepository listeningRoomRepository;
    private final EntityManager entityManager;
    
    @Transactional 
    public ListeningRoom createRoom(Long ownerId) {
        listeningRoomRepository.findAllByOwnerId(ownerId)
            .forEach(r -> deleteRoom(r));

        ListeningRoom room = new ListeningRoom(
            entityManager.getReference(User.class, ownerId)
        );

        return listeningRoomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(ListeningRoom room) {
        for (User member : room.getMembers()) {
            member.setListeningRoom(null);
        }

        listeningRoomRepository.delete(room);
    }
}
