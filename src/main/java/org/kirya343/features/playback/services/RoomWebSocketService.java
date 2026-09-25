package org.kirya343.features.playback.services;

import java.util.Objects;

import org.kirya343.features.playback.dto.PlaybackStateDTO;
import org.kirya343.features.room.dto.RoomDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j 
@RequiredArgsConstructor
public class RoomWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastPlaybackState(String userOpenId, PlaybackStateDTO state) {

        log.debug("Sending state info to user {}, pos {}, qi {}", userOpenId, state.position(), state.entryId());

        messagingTemplate.convertAndSendToUser(
            userOpenId,
            "/queue/playback",
            Objects.requireNonNull(state)
        );
    }

    public void broadcastRoomInfo(String userOpenId, RoomDTO dto) {

        log.debug("Sending room({}) info to user {}", dto.id(), userOpenId);

        messagingTemplate.convertAndSendToUser(
            userOpenId,
            "/queue/room",
            Objects.requireNonNull(dto)
        );
    }
}