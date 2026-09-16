package org.kirya343.features.audio.services.eventhandlers;

import org.kirya343.features.room.dto.commands.RoomCommand;
import org.kirya343.features.audio.services.command.RoomCommandWorker;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j 
@RequiredArgsConstructor
public class PlaybackCommandHandler {

    private final RoomCommandWorker roomCommandWorker;

    @EventListener
    public void handlePlaybackCommand(RoomCommand command) {
        
        log.debug("Handled RoomCommand {}", command.getClass());

        roomCommandWorker.submit(command);
    }
}
