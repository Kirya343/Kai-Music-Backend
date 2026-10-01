package org.kirya343.features.playback.datasource.model;

import java.time.Duration;
import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;
import org.kirya343.features.room.datasource.ListeningRoom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class RoomPlaybackState {
    
    @Id
    private Long roomId;

    @OneToOne
    @MapsId
    private ListeningRoom room;

    @Column(nullable = false)
    private Double position = 0.0;

    @JoinColumn(name = "audio_id")
    private Long currentQueueEntryId;

    @Column(nullable = false)
    private boolean paused = true;

    @UpdateTimestamp
    private Instant lastUpdate;

    private String user;

    public RoomPlaybackState(ListeningRoom room, Long currentQueueEntryId, Double position, boolean paused, String user) {
        this.room = room;
        this.currentQueueEntryId = currentQueueEntryId;
        this.position = position;
        this.paused = paused;
        this.user = user;
    }

    public RoomPlaybackState(ListeningRoom room) {
        this.room = room;
    }

    public double getCurrentPosition() {
        if (paused) {
            return position;
        }
        double elapsed = Duration.between(lastUpdate, Instant.now()).toSeconds();
        return position + elapsed;
    }
}
