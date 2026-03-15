package org.kirya343.core.audio;

import org.springframework.stereotype.Service;

import com.mpatric.mp3agic.Mp3File;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudioService {
    
    public long getDuration(String path) {
        try {

            Mp3File mp3 = new Mp3File(path);

            return mp3.getLengthInSeconds();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
