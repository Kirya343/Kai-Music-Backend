package org.kirya343.features.room.controller;

import org.kirya343.features.audio.services.AudioQueryService;
import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.dto.RoomDTO;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller 
@Slf4j 
@RequiredArgsConstructor 
public class RoomWebSocketController {

    private final AudioQueryService audioQueryService;
    
    @MessageMapping("/room/load")
    @SendToUser("/queue/room")
    public RoomDTO prev(
        @AuthenticationPrincipal UserAuthData authData
    ) {
        log.info("Catched /room/load");
        return audioQueryService.getCurrentRoom(authData);
    }
}
