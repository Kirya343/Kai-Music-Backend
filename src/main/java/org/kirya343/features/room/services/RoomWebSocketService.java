package org.kirya343.features.room.services;

import org.kirya343.features.room.dto.ShortRoomDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j 
@RequiredArgsConstructor
public class RoomWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastRoomInfo(String userOpenId, ShortRoomDTO dto) {
        
        if (dto == null) return;

        log.debug("Sending room({}) info to user {}", dto.id(), userOpenId);

        messagingTemplate.convertAndSendToUser(
            userOpenId,
            "/queue/room",
            dto
        );
    }
}