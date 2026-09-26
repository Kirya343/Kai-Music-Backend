package org.kirya343.features.playback.eventhandlers;

import org.kirya343.features.playback.dto.commands.RoomCommand;
import org.kirya343.features.playback.services.command.PlaybackCommandWorker;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j 
@RequiredArgsConstructor
public class PlaybackCommandHandler {

    private final PlaybackCommandWorker roomCommandWorker;

    @EventListener
    public void handlePlaybackCommand(RoomCommand command) {
        
        log.debug("Handled RoomCommand {}", command.getClass());

        roomCommandWorker.submit(command);
    }
}
