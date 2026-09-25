package org.kirya343.features.playback.services;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.playback.services.cache.RoomPlaybackContext;
import org.kirya343.features.playback.services.cache.RoomPlaybackContextStore;
import org.kirya343.features.playback.services.streaming.AudioStreamWorkerManager;
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
