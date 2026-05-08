package org.kirya343.datasource.model.audio;

import org.kirya343.datasource.model.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
public class AudioFile {

    public AudioFile(
            String name, 
            String path, 
            String format, 
            User owner,
            String title, 
            String artist, 
            String album, 
            String coverUrl,
            Long duration
        ) {
        this.name = name;
        this.path = path;
        this.format = format;
        this.owner = owner;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.coverUrl = coverUrl;
        this.duration = duration;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
  
    private String name;

    private String path;
    private String format;

    @Setter
    private String title;
    @Setter
    private String artist;
    @Setter
    private String album;

    @Setter
    private Long duration;

    @Setter
    private String coverUrl;

    @ManyToOne
    private User owner;
}
