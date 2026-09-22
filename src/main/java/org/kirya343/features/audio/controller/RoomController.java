package org.kirya343.features.audio.controller;

import java.util.List;

import org.kirya343.features.audio.services.AudioQueryService;
import org.kirya343.features.audio.services.playback.RoomWebSocketService;
import org.kirya343.features.presence.services.PresenceService;
import org.kirya343.features.room.services.RoomCommandService;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.audio.datasource.model.RoomPlaybackState;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.audio.datasource.repository.RoomPlaybackStateRepository;
import org.kirya343.features.user.datasource.UserRepository;
import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.dto.MainPageRequest;
import org.kirya343.features.room.dto.RoomDTO;
import org.kirya343.features.room.dto.RoomUpdateDTO;
import org.kirya343.features.room.dto.ShortListeningRoomDTO;
import org.kirya343.features.audio.enums.PlaybackMode;
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

@RestController 
@RequiredArgsConstructor 
@RequestMapping("/room")
public class RoomController {

    private final RoomPlaybackStateRepository roomPlaybackStateRepository;
    private final ListeningRoomRepository listeningRoomRepository;
    private final AudioQueryService audioQueryService;
    private final PresenceService presenceService;
    private final UserRepository userRepository;
    private final RoomCommandService roomCommandService;
    private final RoomWebSocketService roomWebSocketService;

    @GetMapping
    public RoomDTO getCurrentRoom(@AuthenticationPrincipal UserAuthData authData) {
        return audioQueryService.getCurrentRoom(authData);
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
        ListeningRoom room = listeningRoomRepository.findByCode(code).orElseThrow();
        userRepository.updateListeningRoom(authData.id(), room.getId());

        roomWebSocketService.broadcastRoomInfo(authData.openId(), RoomDTO.ofRoom(room));
    }

    @GetMapping("/list/page")
    public MainPageRequest getMainPage() {

        List<ShortListeningRoomDTO> rooms = listeningRoomRepository.findAllShortDTOs();
        long roomsCount = listeningRoomRepository.count();

        return new MainPageRequest(
            rooms,
            presenceService.countAll(),
            roomsCount
        );
    }

    @PatchMapping("/{roomId}/mode")
    public void updatePlaybackMode(@PathVariable Long roomId, @RequestParam PlaybackMode mode) {
        listeningRoomRepository.updatePlaybackMode(roomId, mode);
    }
    
    @GetMapping("/{roomId}/playback-state")
    public PlaybackStateDTO getPlaybackState(
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        RoomPlaybackState state = roomPlaybackStateRepository.findById(roomId).orElseThrow();

        return PlaybackStateDTO.ofState(state);
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
