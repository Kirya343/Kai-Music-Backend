package org.kirya343.datasource.model.user.audio;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.kirya343.datasource.model.user.User;

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
        @Index(name = "idx_room_position", columnList = "room_id, position")
    }
)
@NoArgsConstructor
public class QueueItem {

    public QueueItem(ListeningRoom room, AudioFile audio, Long position, User addedBy) {
        this.room = room;
        this.audio = audio;
        this.position = position;
        this.addedBy = addedBy;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ListeningRoom room;

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
