package org.kirya343.features.audio.services.eventhandlers;

import org.kirya343.features.audio.dto.event.QueueChangedEvent;
import org.kirya343.features.audio.services.cache.RoomPlaybackContextStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j 
@RequiredArgsConstructor
public class QueueEventHandler {

    private final RoomPlaybackContextStore roomPlaybackContextStore;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQueueChanger(QueueChangedEvent event) {      

        roomPlaybackContextStore.reloadAndResendContext(event.roomId());
    }
}
