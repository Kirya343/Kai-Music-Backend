package org.kirya343.datasource.model.audio;

import org.kirya343.datasource.model.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class AudioFile {

    public AudioFile(String name, String path, String format, User owner) {
        this.name = name;
        this.path = path;
        this.format = format;
        this.owner = owner;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
  
    private String name;
    private String path;
    private String format;

    @ManyToOne
    private User owner;
}
