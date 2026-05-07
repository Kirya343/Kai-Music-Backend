package org.kirya343.api.controller.audio;

import org.kirya343.core.audio.AudioFileManager;
import org.kirya343.core.audio.AudioMappingService;
import org.kirya343.core.audio.AudioQueryService;
import org.kirya343.datasource.model.audio.AudioFile;
import org.kirya343.datasource.model.audio.ListeningRoom;
import org.kirya343.datasource.model.audio.QueueItem;
import org.kirya343.datasource.model.audio.RoomPlaybackState;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.repository.audio.AudioFileRepository;
import org.kirya343.datasource.repository.audio.ListeningRoomRepository;
import org.kirya343.datasource.repository.audio.QueueItemRepository;
import org.kirya343.datasource.repository.audio.RoomPlaybackStateRepository;
import org.kirya343.dto.audio.AudioDTO;
import org.kirya343.dto.audio.ListeningRoomDTO;
import org.kirya343.dto.audio.PlaybackStateDTO;
import org.kirya343.dto.audio.QueueItemDTO;
import org.kirya343.dto.audio.ShortListeningRoomDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.enums.PlaybackMode;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioQueryService audioQueryService;
    private final AudioFileRepository audioFileRepository;
    private final AudioMappingService audioMappingService;
    private final EntityManager entityManager;
    private final QueueItemRepository queueItemRepository;
    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomPlaybackStateRepository roomPlaybackStateRepository;
    private final AudioFileManager audioFileManager;

    @GetMapping("/{queueItemId}")
    public ResponseEntity<InputStreamResource> getAudio(
            @AuthenticationPrincipal UserAuthData authData,
            @PathVariable Long queueItemId,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) throws IOException {
        return audioFileManager.getAudio(queueItemId, rangeHeader, authData);
    }

    @GetMapping("/{queueItemId}/info")
    public AudioDTO getAudioInfo(@PathVariable Long queueItemId) {
        AudioFile audio = queueItemRepository.findAudioById(queueItemId);

        return audioMappingService.toDTO(audio);
    }

    @GetMapping("/room")
    public ListeningRoomDTO getCurrentRoom(@AuthenticationPrincipal UserAuthData authData) {
        return audioQueryService.getCurrentRoom(authData);
    }

    @PatchMapping("/room/{roomId}/mode")
    public void updatePlaybackMode(@PathVariable Long roomId, @RequestParam PlaybackMode mode) {
        listeningRoomRepository.updatePlaybackMode(roomId, mode);
    }

    @GetMapping("/library")
    public List<AudioDTO> getUserLibrary(@AuthenticationPrincipal UserAuthData authData) {
        return audioFileRepository.findByOwnerId(authData.id())
            .stream().map(a -> audioMappingService.toDTO(a)).toList();
    }

    @PatchMapping("/room/{roomId}")
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
        return audioMappingService.toDTO(saved);
    }

    @DeleteMapping("/room/{roomId}")
    public void removeFromQueue(
        @PathVariable Long roomId, 
        @RequestParam Long queueItemId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        queueItemRepository.deleteById(queueItemId);
    }

    @PostMapping("/upload")
    public void uploadAudio(
        @RequestParam MultipartFile file,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        audioFileManager.uploadAudio(file, authData);
    }

    @GetMapping("/room/{roomId}/playback-state")
    public PlaybackStateDTO getPlaybackState(
        @PathVariable Long roomId,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        RoomPlaybackState state = roomPlaybackStateRepository.findById(roomId).orElseThrow(
            () -> new EntityNotFoundException("Бэкап трека в комнате не найден"));

        return new PlaybackStateDTO(
            state.getUser(), 
            state.getCurrentQueueEntryId(), 
            state.getCurrentPosition(), 
            state.isPaused()
        );
    }

    @GetMapping("/room/all")
    public List<ShortListeningRoomDTO> getListingRooms() {
        return listeningRoomRepository.findAllShortDTOs();
    }

    @PostMapping("/room")
    public void createRoom(@AuthenticationPrincipal UserAuthData authData) {
        ListeningRoom room = new ListeningRoom(entityManager.getReference(User.class, authData.id()));
        listeningRoomRepository.save(room);
    }
}