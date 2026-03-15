package org.kirya343.core.audio.room;

import java.util.Objects;

import org.kirya343.dto.audio.PlaybackStateDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastPlaybackState(Long roomId, PlaybackStateDTO state) {

        messagingTemplate.convertAndSend(
            "/topic/room/" + roomId,
            Objects.requireNonNull(state)
        );

    }
}