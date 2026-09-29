package org.kirya343.features.playback;

import java.util.HashMap;
import java.util.Map;

import org.kirya343.features.audio.dto.AudioChunk;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service 
@Slf4j
@RequiredArgsConstructor 
public class PlaybackWebsocketService {

    private final SimpMessagingTemplate messagingTemplate;
    
    public void sendChunk(String user, AudioChunk chunk, Long entryId) {
        
        Map<String, Object> headers = new HashMap<>();

        log.debug(
            "Sending {} chunk to {}: chunk={}, audio={}", 
            chunk.initialization() ? "initialization" : "media", 
            user, 
            chunk.sequence());

        headers.put("content-type", "audio/mp4");
        headers.put("sequence", chunk.sequence());
        headers.put("duration", chunk.durationMs());
        headers.put("entry-id", entryId);
        headers.put("initialization", chunk.initialization());

        messagingTemplate.convertAndSendToUser(
            user,
            "/queue/audio",
            chunk.data(),
            headers
        );
    }
}
