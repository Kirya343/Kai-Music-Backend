package org.kirya343.features.audio.services.playback;

import org.kirya343.features.audio.services.cache.RoomPlaybackContext;
import org.kirya343.features.audio.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.audio.services.streaming.AudioStreamWorkerManager;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class RoomSessionService {

    private final RoomPlaybackContextStore roomPlaybackContextStore;
    private final RoomWebSocketService roomWebSocketService;
    private final AudioStreamWorkerManager audioStreamWorkerManager;
    
    public void initializeRoom(Long roomId, UserAuthData authData) {

        roomPlaybackContextStore.computeIfAbsent(roomId).getListeners().add(authData.openId());

        RoomPlaybackContext roomContext = roomPlaybackContextStore.get(roomId);
        roomWebSocketService.broadcastRoomInfo(authData.openId(), roomContext.getFullRoom());
        audioStreamWorkerManager.getWorker(roomId).handleUserConnected(authData.openId());
    }
}
