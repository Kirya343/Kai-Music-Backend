package org.kirya343.features.audio.services.util;

import org.kirya343.features.audio.datasource.model.AudioFile;

import com.mpatric.mp3agic.Mp3File;

public class AudioMp3Service {

    public static Long getDuration(AudioFile audio) {
        Long durationFromDB = audio.getDuration();
        return durationFromDB != null ? durationFromDB : AudioMp3Service.calculateDuration(audio.getPath());
    }
    
    public static long calculateDuration(String path) {
        try {

            Mp3File mp3 = new Mp3File(path);

            return mp3.getLengthInSeconds();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
