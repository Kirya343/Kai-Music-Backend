package org.kirya343.features.room.services;

import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.user.datasource.User;
import org.kirya343.features.user.datasource.UserRepository;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.PlaybackWebsocketService;
import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.playback.services.RoomSessionService;
import org.kirya343.features.playback.services.streaming.RoomStreamWorkerManager;
import org.kirya343.features.playlist.dto.PlaylistDTO;
import org.kirya343.features.playlist.service.PlaylistWebSocketService;
import org.kirya343.features.room.dto.ShortRoomDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@Slf4j 
@RequiredArgsConstructor 
public class RoomCommandService {

    private final ListeningRoomRepository listeningRoomRepository;
    private final UserRepository userRepository;
    private final RoomWebSocketService roomWebSocketService;
    private final EntityManager entityManager;
    private final RoomSessionService roomSessionService;
    private final PlaylistWebSocketService playlistWebSocketService;
    private final RoomStreamWorkerManager roomStreamWorkerManager;
    private final PlaybackWebsocketService playbackWebsocketService;
    
    @Transactional 
    public ListeningRoom createRoom(UserAuthData authData) {
        listeningRoomRepository.findAllByOwnerId(authData.id())
            .forEach(r -> deleteRoom(r));

        ListeningRoom room = new ListeningRoom(
            entityManager.getReference(User.class, authData.id())
        );

        ListeningRoom saved = listeningRoomRepository.save(room);
        userRepository.updateListeningRoom(authData.id(), saved.getId());

        roomWebSocketService.broadcastRoomInfo(authData.openId(), ShortRoomDTO.ofRoom(saved));

        return saved;
    }

    @Transactional
    public void deleteRoom(ListeningRoom room) {
        for (User member : room.getMembers()) {
            member.setListeningRoom(null);
        }

        listeningRoomRepository.delete(room);
    }

    public void connectUserToRoom(ListeningRoom room, UserAuthData authData) {
        log.debug("Пользователь {} присоединяется к комнате {}", authData.name(), room.getId());
        userRepository.updateListeningRoom(authData.id(), room.getId());

        roomWebSocketService.broadcastRoomInfo(authData.openId(), ShortRoomDTO.ofRoom(room));
        playbackWebsocketService.broadcastPlaybackState(authData.openId(), PlaybackStateDTO.ofState(room.getPlaybackState()));
        playlistWebSocketService.broadcastRoomPlaylist(authData.openId(), PlaylistDTO.ofPlaylist(room.getPlaylist()));

        roomSessionService.initializeRoom(room.getId(), authData);

        // Reloads worker
        roomStreamWorkerManager.getWorker(room.getId());
    }
}
