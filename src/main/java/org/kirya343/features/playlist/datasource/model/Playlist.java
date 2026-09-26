package org.kirya343.features.playlist.datasource.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.kirya343.features.playback.enums.PlaybackMode;
import org.kirya343.features.user.datasource.User;
import org.kirya343.infrastructure.common.config.datasource.Constants;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Getter 
@NoArgsConstructor
public class Playlist {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 6)
    private String code = NanoIdUtils.randomNanoId(
        NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
        Constants.ALPHANUMERIC,
        6
    );
    
    @ManyToOne
    private User owner;

    @Setter
    private String title;

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QueueItem> queue = new ArrayList<>();

    @Setter
    @Enumerated(EnumType.STRING)
    private PlaybackMode playbackMode = PlaybackMode.NORMAL;

    @CreationTimestamp
    private Instant createdAt;

    public Playlist(String title, User owner) {
        this.title = title;
        this.owner = owner;
    }
}
