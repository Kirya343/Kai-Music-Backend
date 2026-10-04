package org.kirya343.features.room.controller;

import java.util.List;

import org.kirya343.features.presence.services.PresenceService;
import org.kirya343.features.room.services.RoomCommandService;
import org.kirya343.features.room.services.RoomQueryService;
import org.kirya343.features.room.services.RoomWebSocketService;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.user.datasource.UserRepository;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.services.RoomSessionService;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playback.services.streaming.UserAudioStreamWorkerManager;
import org.kirya343.features.playlist.dto.PlaylistDTO;
import org.kirya343.features.playlist.service.PlaylistWebSocketService;
import org.kirya343.features.room.dto.MainPageRequest;
import org.kirya343.features.room.dto.RoomDTO;
import org.kirya343.features.room.dto.RoomUpdateDTO;
import org.kirya343.features.room.dto.ShortRoomDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController 
@Slf4j 
@RequiredArgsConstructor 
@RequestMapping("/room")
public class RoomController {

    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomQueryService roomQueryService;
    private final PresenceService presenceService;
    private final UserRepository userRepository;
    private final RoomCommandService roomCommandService;
    private final RoomSessionService roomSessionService;
    private final UserAudioStreamWorkerManager userAudioStreamWorkerManager;
    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final RoomWebSocketService roomWebSocketService;
    private final PlaylistWebSocketService playlistWebSocketService;

    @GetMapping
    public RoomDTO getCurrentRoom(@AuthenticationPrincipal UserAuthData authData) {
        return roomQueryService.getCurrentRoom(authData);
    }

    @PostMapping
    public void createRoom(@AuthenticationPrincipal UserAuthData authData) {
        roomCommandService.createRoom(authData);
    }

    @PostMapping("/join")
    public void setUserRoom(
        @RequestParam String code,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        ListeningRoom prevRoom = listeningRoomRepository.findRoomByUserId(authData.id()).orElse(null);
        if (prevRoom != null) {
            RoomPlaybackContext context = roomPlaybackContextStore.get(prevRoom.getId());
            if (context != null) {
                context.getListeners().remove(authData.openId());
            }
            userAudioStreamWorkerManager.removeWorker(authData.openId());
        }

        ListeningRoom newRoom = listeningRoomRepository.findByCode(code).orElseThrow();

        log.info("Пользователь {} присоединяется к комнате {}", authData.name(), newRoom.getId());
        userRepository.updateListeningRoom(authData.id(), newRoom.getId());

        roomWebSocketService.broadcastRoomInfo(authData.openId(), ShortRoomDTO.ofRoom(newRoom));
        playlistWebSocketService.broadcastRoomPlaylist(authData.openId(), PlaylistDTO.ofPlaylist(newRoom.getPlaylist()));

        roomSessionService.initializeRoom(newRoom.getId(), authData);
    }

    @PostMapping("/leave")
    public void leaveRoom(
        @AuthenticationPrincipal UserAuthData authData
    ) {

        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id()).orElse(null);
        if (room != null) {
            RoomPlaybackContext context = roomPlaybackContextStore.get(room.getId());
            if (context != null) {
                context.getListeners().remove(authData.openId());
            }
            userAudioStreamWorkerManager.removeWorker(authData.openId());
        }

        log.info("Пользователь {} покинул комнату {}", authData.name(), room.getId());
        userRepository.updateListeningRoom(authData.id(), null);
    }

    @GetMapping("/list/page")
    public MainPageRequest getMainPage() {

        List<ShortRoomDTO> rooms = listeningRoomRepository.findAllShortDTOs();
        long roomsCount = listeningRoomRepository.count();

        return new MainPageRequest(
            rooms,
            presenceService.countAll(),
            roomsCount
        );
    }

    @PatchMapping("/{roomId}")
    public void updateRoom(
        @PathVariable Long roomId,
        @RequestBody RoomUpdateDTO roomDto,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        
        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Комнаты не существует"));

        if (roomDto.title() != null) room.setTitle(roomDto.title());

        listeningRoomRepository.save(room);
    }
}
