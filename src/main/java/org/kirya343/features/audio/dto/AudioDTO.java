package org.kirya343.features.audio.dto;

import java.util.Collection;
import java.util.List;

import org.kirya343.features.audio.datasource.model.AudioFile;

public record AudioDTO(
    Long id,
    String name,
    String format,
    String title,
    String artist,
    String album,
    Long duration,
    String coverUrl
) {
    public static AudioDTO ofAudioFile(AudioFile audio) {

        if (audio == null) return null;
        
        return new AudioDTO(
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

    public static List<AudioDTO> ofList(Collection<AudioFile> audios) {
        return audios.stream().map(a -> AudioDTO.ofAudioFile(a)).toList();
    }
}