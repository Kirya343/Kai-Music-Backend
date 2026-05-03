package org.kirya343.datasource.model.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.kirya343.core.config.Constants;
import org.springframework.lang.NonNull;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

@Getter
@Entity
@NoArgsConstructor
public class Message {

    public Message(String chatOpenId,
                   String senderOpenId,
                   String content) {
        this.chatOpenId = chatOpenId;
        this.senderOpenId = senderOpenId;
        this.content = content;
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

    private String chatOpenId;

    private String senderOpenId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private Instant timestamp;

    @Setter
    @Column(name = "is_read")
    private boolean read = false;

    @NonNull
    public static Message create(String chatOpenId, String senderOpenId, String content) {
        Message m = new Message(
            chatOpenId,
            senderOpenId,
            content
        );
        return m;
    }
}
