package org.kirya343.api.controller.audio;

import java.util.List;

import org.kirya343.core.audio.AudioQueryService;
import org.kirya343.core.presence.PresenceService;
import org.kirya343.core.room.RoomCommandService;
import org.kirya343.datasource.model.audio.AudioFile;
import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.model.audio.QueueItem;
import org.kirya343.datasource.model.audio.RoomPlaybackState;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.datasource.repository.audio.QueueItemRepository;
import org.kirya343.datasource.repository.audio.RoomPlaybackStateRepository;
import org.kirya343.datasource.repository.user.UserRepository;
import org.kirya343.dto.audio.PlaybackStateDTO;
import org.kirya343.dto.audio.QueueItemDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.room.MainPageRequest;
import org.kirya343.dto.room.RoomDTO;
import org.kirya343.dto.room.ShortListeningRoomDTO;
import org.kirya343.enums.PlaybackMode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RestController 
@RequiredArgsConstructor 
@RequestMapping("/room")
public class RoomController {

    private final RoomPlaybackStateRepository roomPlaybackStateRepository;
    private final ListeningRoomRepository listeningRoomRepository;
    private final AudioQueryService audioQueryService;
    private final EntityManager entityManager;
    private final QueueItemRepository queueItemRepository;
    private final PresenceService presenceService;
    private final UserRepository userRepository;
    private final RoomCommandService roomCommandService;

    @GetMapping
    public RoomDTO.Get getCurrentRoom(@AuthenticationPrincipal UserAuthData authData) {
        return audioQueryService.getCurrentRoom(authData);
    }

    @PostMapping
    public void createRoom(@AuthenticationPrincipal UserAuthData authData) {
        roomCommandService.createRoom(authData.id());
    }

    @PostMapping("/join")
    public void setUserRoom(
        @RequestParam String code,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        ListeningRoom room = listeningRoomRepository.findByCode(code).orElseThrow();
        userRepository.updateListeningRoom(authData.id(), room.getId());
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

        return new PlaybackStateDTO(
            state.getUser(), 
            state.getCurrentQueueEntryId(), 
            state.getCurrentPosition(), 
            state.isPaused()
        );
    }

    @PatchMapping("/{roomId}/queue")
    public QueueItemDTO addToQueue(
        @PathVariable Long roomId, 
        @RequestParam Long audioId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        QueueItem qi = new QueueItem(
            entityManager.getReference(ListeningRoom.class, roomId), 
            entityManager.getReference(AudioFile.class, audioId),
            queueItemRepository.findMaxPosition() + 50,
            entityManager.getReference(User.class, authData.id())
        );

        QueueItem saved = queueItemRepository.save(qi);
        return QueueItemDTO.ofQueueItem(saved);
    }

    @Transactional 
    @DeleteMapping("/{roomId}/queue")
    public void removeFromQueue(
        @PathVariable Long roomId, 
        @RequestParam Long queueItemId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        queueItemRepository.deleteByIdAndRoomId(queueItemId, roomId);
    }

    @PatchMapping("/{roomId}")
    public void updateRoom(
        @PathVariable Long roomId,
        @RequestBody RoomDTO.Update roomDto,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        
        ListeningRoom room = listeningRoomRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Комнаты не существует"));

        if (roomDto.title() != null) room.setTitle(roomDto.title());

        listeningRoomRepository.save(room);
    }
}
