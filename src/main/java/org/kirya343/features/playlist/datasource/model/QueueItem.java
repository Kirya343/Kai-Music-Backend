package org.kirya343.features.playlist.datasource.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.kirya343.features.audio.datasource.AudioFile;
import org.kirya343.features.user.datasource.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Table(
    name = "queue_items",
    indexes = {
        @Index(name = "idx_playlist_position", columnList = "playlist_id, position")
    }
)
@NoArgsConstructor
public class QueueItem {

    public QueueItem(Playlist playlist, AudioFile audio, Long position, User addedBy) {
        this.playlist = playlist;
        this.audio = audio;
        this.position = position;
        this.addedBy = addedBy;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Playlist playlist;

    @ManyToOne(optional = false)
    private AudioFile audio;

    @Setter
    @Column(nullable = false)
    private Long position;

    @ManyToOne
    private User addedBy;

    @CreationTimestamp
    private Instant createdAt = Instant.now();
}
