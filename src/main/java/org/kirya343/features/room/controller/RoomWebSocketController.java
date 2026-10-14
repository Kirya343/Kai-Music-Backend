package org.kirya343.features.room.controller;

import org.kirya343.features.authentication.dto.UserAuthData;
import org.kirya343.features.room.datasource.ListeningRoom;
import org.kirya343.features.room.datasource.ListeningRoomRepository;
import org.kirya343.features.room.services.RoomCommandService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller 
@Slf4j 
@RequiredArgsConstructor 
public class RoomWebSocketController {

    private final ListeningRoomRepository listeningRoomRepository;
    private final RoomCommandService roomCommandService;
    
    @MessageMapping ("/room/reconnect")
    public void setUserRoom(
        @AuthenticationPrincipal UserAuthData authData
    ) {
        ListeningRoom room = listeningRoomRepository.findRoomByUserId(authData.id()).orElse(null);

        roomCommandService.connectUserToRoom(room, authData);
    }
}
