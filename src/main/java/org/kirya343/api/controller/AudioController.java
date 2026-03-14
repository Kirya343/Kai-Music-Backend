package org.kirya343.api.controller;

import org.kirya343.core.audio.AudioMappingService;
import org.kirya343.core.audio.AudioQueryService;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.model.user.audio.AudioFile;
import org.kirya343.datasource.model.user.audio.ListeningRoom;
import org.kirya343.datasource.model.user.audio.QueueItem;
import org.kirya343.datasource.repository.audio.AudioFileRepository;
import org.kirya343.datasource.repository.audio.QueueItemRepository;
import org.kirya343.dto.audio.AudioDTO;
import org.kirya343.dto.audio.ListeningRoomDTO;
import org.kirya343.dto.audio.QueueItemDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioQueryService audioQueryService;
    private final AudioFileRepository audioFileRepository;
    private final AudioMappingService audioMappingService;
    private final EntityManager entityManager;
    private final QueueItemRepository queueItemRepository;

    @GetMapping("/{audioId}")
    public ResponseEntity<InputStreamResource> getAudio(
        @AuthenticationPrincipal UserAuthData authData, 
        @PathVariable Long audioId
    ) throws IOException {

        AudioFile audio = audioFileRepository.findAudioInUserRoom(authData.id(), audioId)
            .orElseThrow(() -> new AccessDeniedException("Нет доступа к этому треку"));

        File audioFile = new File(audio.getPath()); // путь к вашему файлу
        FileInputStream fis = new FileInputStream(audioFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + audioFile.getName())
                .contentLength(audioFile.length())
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(new InputStreamResource(fis));
    }

    @GetMapping("/room")
    public ListeningRoomDTO getCurrentRoom(@AuthenticationPrincipal UserAuthData authData) {
        return audioQueryService.getCurrentRoom(authData);
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

    @PostMapping("/upload")
    public void uploadAudio(
        @RequestParam MultipartFile file,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        String originalName = file.getOriginalFilename();
        String title = originalName;
        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        if (originalName != null && originalName.contains(".")) {
            title = originalName.substring(0, originalName.lastIndexOf("."));
        }

        String name = UUID.randomUUID().toString() + extension;

        try {
            Path path = Paths.get("music/" + name);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            AudioFile audio = new AudioFile(
                title, 
                "music/" + name, 
                entityManager.getReference(User.class, authData.id())
            );

            audioFileRepository.save(audio);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}