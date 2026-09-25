package org.kirya343.features.audio.controller;

import org.kirya343.infrastructure.security.services.UserAuthDataService;
import org.kirya343.features.audio.datasource.model.AudioFile;
import org.kirya343.features.audio.datasource.repository.AudioFileRepository;
import org.kirya343.features.audio.datasource.repository.QueueItemRepository;
import org.kirya343.features.audio.dto.AudioDTO;
import org.kirya343.features.audio.dto.AudioUpdateDTO;
import org.kirya343.features.audio.services.AudioCommandService;
import org.kirya343.features.audio.services.storage.AudioFileManager;
import org.kirya343.features.audio.services.storage.AudioStorageService;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioFileRepository audioFileRepository;
    private final QueueItemRepository queueItemRepository;
    private final AudioFileManager audioFileManager;
    private final UserAuthDataService userAuthDataService;
    private final AudioStorageService audioStorageService;
    private final AudioCommandService audioCommandService;

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
        AudioFile audio = queueItemRepository.findAudioById(queueItemId).orElseThrow();

        return AudioDTO.ofAudioFile(audio);
    }

    @GetMapping("/library")
    public List<AudioDTO> getUserLibrary(@AuthenticationPrincipal UserAuthData authData) {
        List<AudioFile> audios = audioFileRepository.findByOwnerId(authData.id());
        return AudioDTO.ofList(audios);
    }

    @PostMapping("/recognize/{audioId}")
    public AudioDTO recognize(
        @PathVariable Long audioId, 
        @AuthenticationPrincipal UserAuthData authData
    ) {
        try {
            return audioCommandService.recognize(audioId, authData);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/upload")
    public void uploadAudio(
        @RequestParam MultipartFile file,
        @RequestParam(required = false) String apiKey,
        @AuthenticationPrincipal UserAuthData authData
    ) {

        if (apiKey != null) {
            authData = userAuthDataService.load(apiKey);
        }

        audioFileManager.uploadAudio(file, authData);
    }

    @PatchMapping("/{audioId}")
    public void updateAudio(
        @PathVariable Long audioId,
        @RequestBody AudioUpdateDTO dto,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        
        AudioFile audio = audioFileRepository.findById(audioId).orElseThrow(
            () -> new EntityNotFoundException("Трека не существует"));

        if (dto.album() != null && dto.album().length() > 0) audio.setAlbum(dto.album());
        if (dto.artist() != null && dto.artist().length() > 0) audio.setArtist(dto.artist());
        if (dto.title() != null && dto.title().length() > 0) audio.setTitle(dto.title());
        if (dto.coverUrl() != null && dto.coverUrl().length() > 0) audio.setCoverUrl(dto.coverUrl());

        audioFileRepository.save(audio);
    }

    @DeleteMapping ("/{audioId}")
    public void deleteAudio(
        @PathVariable Long audioId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        
        AudioFile audio = audioFileRepository.findById(audioId).orElseThrow(
            () -> new EntityNotFoundException("Трека не существует"));

        try {
            audioStorageService.deleteAudio(audio.getPath());
        } catch (Exception e) {
            throw e;
        }

        audioFileRepository.save(audio);
    }
}