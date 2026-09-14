package org.kirya343.dto.audio;

import java.util.Collection;
import java.util.List;

import org.kirya343.datasource.model.audio.AudioFile;

public class AudioDTO {

    public record Get(
        Long id,
        String name,
        String format,
        String title,
        String artist,
        String album,
        Long duration,
        String coverUrl
    ) {
        public static AudioDTO.Get ofAudioFile(AudioFile audio) {
            return new AudioDTO.Get(
                audio.getId(), 
                audio.getName(),
                audio.getFormat(),
                audio.getTitle(),
                audio.getArtist(),
                audio.getAlbum(),
                audio.getDuration(),
                audio.getCoverUrl()
            );
        }

        public static List<AudioDTO.Get> ofList(Collection<AudioFile> audios) {
            return audios.stream().map(a -> AudioDTO.Get.ofAudioFile(a)).toList();
        }
    }

    public record Update(
        String title,
        String artist,
        String album,
        String coverUrl
    ) {}
}
