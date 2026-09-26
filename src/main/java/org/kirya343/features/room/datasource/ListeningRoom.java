package org.kirya343.features.room.datasource;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.kirya343.infrastructure.common.config.datasource.Constants;
import org.kirya343.features.user.datasource.User;
import org.kirya343.features.playback.datasource.model.RoomPlaybackState;
import org.kirya343.features.playlist.datasource.model.Playlist;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
public class ListeningRoom {

    public ListeningRoom(User owner) {
        this.owner = owner;
        this.members = new HashSet<>();
        this.members.add(owner);
        this.title = owner.getName() + "\'s room";

        this.playlist = new Playlist(title + " playlist", owner);
        this.playbackState = new RoomPlaybackState(this);
    }
    
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

    @OneToMany(mappedBy = "listeningRoom")
    private Set<User> members = new HashSet<>();

    @ManyToOne(
        cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
        optional = false
    )
    @JoinColumn(nullable = false)
    private Playlist playlist = new Playlist(title + " playlist", owner);
    
    @CreationTimestamp
    private Instant createdAt;

    @OneToOne(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private RoomPlaybackState playbackState;
}
