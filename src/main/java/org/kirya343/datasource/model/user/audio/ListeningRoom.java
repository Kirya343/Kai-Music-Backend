package org.kirya343.datasource.model.user.audio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.kirya343.datasource.model.user.User;
import org.kirya343.enums.PlaybackMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne
    private User owner;

    @OneToMany(mappedBy = "listeningRoom")
    private Set<User> members = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<QueueItem> queue = new ArrayList<>();
    
    @Setter
    @Enumerated(EnumType.STRING)
    private PlaybackMode playbackMode = PlaybackMode.NORMAL;
    
    @CreationTimestamp
    private Instant createdAt;

    @OneToOne(mappedBy = "room")
    private RoomPlaybackState playbackState;
}
