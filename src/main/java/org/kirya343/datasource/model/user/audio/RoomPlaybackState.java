package org.kirya343.datasource.model.user.audio;

import java.time.Duration;
import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

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
    private Long position = 0L;

    @JoinColumn(name = "audio_id")
    private Long currentTrackId;

    @Column(nullable = false)
    private boolean paused = true;

    @UpdateTimestamp
    private Instant lastUpdate;

    private String user;

    public RoomPlaybackState(ListeningRoom room, Long currentTrackId, Long position, boolean paused, String user) {
        this.room = room;
        this.currentTrackId = currentTrackId;
        this.position = position;
        this.paused = paused;
        this.user = user;
    }

    public RoomPlaybackState(ListeningRoom room) {
        this.room = room;
    }

    public long getCurrentPosition() {
        if (paused) {
            return position;
        }
        long elapsed = Duration.between(lastUpdate, Instant.now()).toSeconds();
        return position + elapsed;
    }
}
