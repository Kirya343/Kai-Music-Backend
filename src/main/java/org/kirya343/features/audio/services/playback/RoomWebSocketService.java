package org.kirya343.features.audio.services.playback;

import java.util.Objects;

import org.kirya343.features.audio.dto.PlaybackStateDTO;
import org.kirya343.features.room.dto.RoomDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastPlaybackState(Long roomId, PlaybackStateDTO state) {

        messagingTemplate.convertAndSend(
            "/topic/room/playback/" + roomId,
            Objects.requireNonNull(state)
        );
    }

    public void broadcastRoomInfo(String userOpenId, RoomDTO.Get dto) {

        messagingTemplate.convertAndSendToUser(
            userOpenId,
            "/queue/room",
            Objects.requireNonNull(dto)
        );
    }
}