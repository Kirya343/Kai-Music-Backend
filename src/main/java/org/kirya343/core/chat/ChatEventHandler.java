package org.kirya343.core.chat;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.kirya343.dto.chat.ChatDetails;
import org.kirya343.dto.chat.ChatsLoadedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatEventHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatQueryService chatQueryService;
    
    @EventListener
    @Async
    public void handleChatsLoaded(ChatsLoadedEvent event) {

        List<ChatDetails> details = chatQueryService.getChatDetails(event.authData().id(), event.chats());

        messagingTemplate.convertAndSendToUser(
            event.authData().openId(), 
            "/queue/chats/details",
            details
        );
    }
}
