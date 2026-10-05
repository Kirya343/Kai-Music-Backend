package org.kirya343.features.audio.services;

import java.io.IOException;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.audio.datasource.AudioFileRepository;
import org.kirya343.features.audio.dto.AudioDTO;
import org.kirya343.features.audio.dto.AudioUpdateDTO;
import org.kirya343.features.audio.services.recognition.AcrCloudRecognitionService;
import org.kirya343.features.audio.services.recognition.AcrCloudResponse;
import org.kirya343.features.audio.services.storage.AudioStorageService;
import org.kirya343.features.audio.services.util.M4aBuilder;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@Slf4j 
@RequiredArgsConstructor 
public class AudioCommandService {

    private final AudioStorageService audioStorageService;
    private final AudioFileRepository audioFileRepository;
    private final AcrCloudRecognitionService recognitionService;
    private final AudioWebsocketService audioWebsocketService;
    private final ObjectMapper objectMapper;
    private final M4aBuilder m4aBuilder;
    
    public void recognize(
            Long audioId,
            UserAuthData authData
    ) throws IOException {

        log.info("Starting audio recognition: audioId={}", audioId);

        AudioFile audioFile = audioFileRepository
                .findById(audioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Audio file not found: " + audioId
                ));

        log.info(
                "Audio loaded: audioId={}, path={}",
                audioId,
                audioFile.getPath()
        );

        byte[] m4a = m4aBuilder.buildRecognizeM4a(audioFile);

        long recognitionStartedAt = System.currentTimeMillis();

        log.info(
                "Sending audio to ACRCloud: audioId={}, bytes={}",
                audioId,
                m4a.length
        );

        AcrCloudResponse.Music music = null;

        try {
            String resultStr = recognitionService.recognize(m4a);

            AcrCloudResponse response =
                    objectMapper.readValue(resultStr, AcrCloudResponse.class);

            music = response.metadata()
                            .music()
                            .getFirst();

        } catch (Exception e) {
            log.error(
                    "ACRCloud recognition failed: audioId={}, time={}ms",
                    audioId,
                    System.currentTimeMillis() - recognitionStartedAt,
                    e
            );

            throw e;
        }

        AudioUpdateDTO update = new AudioUpdateDTO(
            music.title(), 
            String.join(", ", music.artists().stream().map(a -> a.name()).toList()), 
            music.title(), 
            null);

        updateAudio(audioId, update, authData);

    }

    public void updateAudio(
        Long audioId,
        AudioUpdateDTO dto,
        UserAuthData authData
    ) {
        
        AudioFile audio = audioFileRepository.findById(audioId).orElseThrow(
            () -> new EntityNotFoundException("Трека не существует"));

        if (dto.album() != null && dto.album().length() > 0) audio.setAlbum(dto.album());
        if (dto.artist() != null && dto.artist().length() > 0) audio.setArtist(dto.artist());
        if (dto.title() != null && dto.title().length() > 0) audio.setTitle(dto.title());
        if (dto.coverUrl() != null && dto.coverUrl().length() > 0) audio.setCoverUrl(dto.coverUrl());

        audioFileRepository.save(audio);

        audioWebsocketService.broadcastAudio(authData.openId(), AudioDTO.ofAudioFile(audio));
    }

    public void deleteAudio(Long audioId) {
        AudioFile audio = audioFileRepository.findById(audioId).orElseThrow(
            () -> new EntityNotFoundException("Трека не существует"));

        try {
            audioStorageService.deleteAudio(audio.getPath());
        } catch (Exception e) {
            log.debug("Error while deleting audio-chunks");
            throw e;
        }

        audioFileRepository.save(audio);
    }
}
