package org.kirya343.features.audio.services;

import org.kirya343.features.audio.dto.AudioDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@Slf4j
@RequiredArgsConstructor 
public class AudioWebsocketService {

    private final SimpMessagingTemplate messagingTemplate;
    
    public void broadcastAudio(String userOpenId, AudioDTO dto) {

        if (dto == null) return;

        log.debug("Sending audio({}) info to user {}", dto.id(), userOpenId);

        messagingTemplate.convertAndSendToUser(
            userOpenId,
            "/queue/audios",
            dto
        );
    }
}
