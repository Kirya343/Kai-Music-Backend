package org.kirya343.datasource.model.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.kirya343.core.config.Constants;
import org.kirya343.datasource.model.user.User;
import org.kirya343.enums.chat.ChatStatus;
import org.kirya343.enums.chat.ChatType;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

@Getter
@Entity
@NoArgsConstructor
public class Chat {

    public Chat(
        Set<User> users,
        ChatType chatType,
        Long targetId
    ) {
        for (User user : users) {
            ChatParticipant participant = new ChatParticipant(this, user);
            participants.add(participant);
        }
        this.chatType = chatType;
        this.targetId = targetId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false, unique = true)
    private String openId = NanoIdUtils.randomNanoId(
        NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
        Constants.ALPHANUMERIC,
        20
    );

    @OneToMany(
        mappedBy = "chat", 
        cascade = CascadeType.ALL, 
        orphanRemoval = true, 
        fetch = FetchType.EAGER
    )
    private Set<ChatParticipant> participants = new HashSet<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    @Setter
    @Enumerated(EnumType.STRING)
    private ChatStatus status = ChatStatus.TEMPORARY;

    @Enumerated(EnumType.STRING)
    private ChatType chatType;

    private Long targetId;

    @Transient
    private long unreadCount;
}

