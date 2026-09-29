package org.kirya343.features.audio.services;

import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.audio.datasource.AudioFileRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioQueryService {

    private final AudioFileRepository audioFileRepository;

    public AudioFile getAudioByQueueItem(Long entryId) {
        AudioFile audioFile = audioFileRepository
            .findAudioByQueueItem(entryId)
            .orElseThrow();

        return audioFile;
    }
}
