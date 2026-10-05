package org.kirya343.features.audio.controller;

import org.kirya343.infrastructure.security.services.UserAuthDataService;
import org.kirya343.features.audio.dto.AudioDTO;
import org.kirya343.features.audio.dto.AudioUpdateDTO;
import org.kirya343.features.audio.services.AudioCommandService;
import org.kirya343.features.audio.services.AudioQueryService;
import org.kirya343.features.audio.services.storage.AudioFileManager;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioFileManager audioFileManager;
    private final UserAuthDataService userAuthDataService;
    private final AudioCommandService audioCommandService;
    private final AudioQueryService audioQueryService;

    @GetMapping("/library")
    public List<AudioDTO> getUserLibrary(@AuthenticationPrincipal UserAuthData authData) {
        return audioQueryService.getUserLibrary(authData.id());
    }

    @PostMapping("/recognize/{audioId}")
    public void recognize(
        @PathVariable Long audioId, 
        @AuthenticationPrincipal UserAuthData authData
    ) {
        try {
            audioCommandService.recognize(audioId, authData);
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
        audioCommandService.updateAudio(audioId, dto, authData);
    }

    @DeleteMapping("/{audioId}")
    public void deleteAudio(
        @PathVariable Long audioId,
        @AuthenticationPrincipal UserAuthData authData
    ) {
        audioCommandService.deleteAudio(audioId);
    }
}