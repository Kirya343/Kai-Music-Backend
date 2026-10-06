package org.kirya343.features.audio.services;

import java.util.List;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.audio.datasource.AudioFileRepository;
import org.kirya343.features.audio.dto.AudioDTO;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioQueryService {

    private final AudioFileRepository audioFileRepository;

    public List<AudioDTO> getUserLibrary(Long userId) {
        List<AudioFile> audios = audioFileRepository.findByOwnerId(userId);
        return AudioDTO.ofList(audios);
    }

    public AudioFile getAudioByQueueItem(Long entryId) {
        AudioFile audioFile = audioFileRepository
            .findAudioByQueueItem(entryId)
            .orElseThrow();

        return audioFile;
    }
}
