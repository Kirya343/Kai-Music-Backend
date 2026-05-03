package org.kirya343.api.controller.chat;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.kirya343.dto.auth.UserAuthData;
import org.kirya343.dto.chat.ChatDTO;
import org.kirya343.dto.chat.MessageDTO;
import org.kirya343.core.chat.ChatCommandService;
import org.kirya343.core.chat.ChatQueryService;
import org.kirya343.dto.user.ShortUserDTO;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final ChatQueryService chatQueryService;
    private final ChatCommandService chatCommandService;

    @MessageMapping("/chat.message-send")
    //@PreAuthorize("hasAuthority('CHAT_SEND_MESSAGE')")
    public void sendMessage(
        MessageDTO messageDTO, 
        @AuthenticationPrincipal UserAuthData authData
    ) throws AccessDeniedException {
        chatCommandService.sendMessage(messageDTO, authData);
    }

    @MessageMapping("/chat.loadMessages/{chatOpenId}")
    @SendToUser("/queue/chat/history.messages/{chatOpenId}")
    //@PreAuthorize("hasAuthority('CHAT_LOAD_HISTORY')")
    public List<MessageDTO> loadMessagesForChat(
        @DestinationVariable String chatOpenId, 
        @AuthenticationPrincipal UserAuthData authData
    ) {
        return chatQueryService.getMessagesByChatId(chatOpenId, authData);
    }

    @MessageMapping("/chat.markAsRead/{chatOpenId}")
   // @PreAuthorize("hasAuthority('CHAT_MARK_AS_READ')")
    public void markAsRead(
        @DestinationVariable String chatOpenId, 
        @AuthenticationPrincipal UserAuthData authData
    ) {
        chatCommandService.markMessagesAsRead(chatOpenId, authData);
    }

    @MessageMapping("/chat.get-chats")
    @SendToUser("/queue/chats")
    //@PreAuthorize("hasAuthority('CHAT_GET_CHATS')")
    public List<ChatDTO> getChats(@AuthenticationPrincipal UserAuthData authData) {
        return chatQueryService.getChatsDTOForUser(authData);
    }

    @Transactional
    @MessageMapping("/chat.get-interlocutor-info/{chatId}")
    @SendToUser("/queue/chat/interlocutor-info")
    //@PreAuthorize("hasAuthority('CHAT_GET_INTERLOCUTOR')")
    public Map<Long, List<ShortUserDTO>> getChatInterlocutors(
        @DestinationVariable Long chatId, 
        @AuthenticationPrincipal UserAuthData authData
    ) {
        return Map.of(chatId, chatQueryService.getChatInterlocutors(chatId, authData));
    }

    @MessageMapping("/messages.get-unread")
    @SendToUser("/queue/chat/messages")
    public List<MessageDTO> getChatUnreadMessages(@AuthenticationPrincipal UserAuthData authData) {
        logger.debug("Ауфдата: {}", authData.toString());
        return chatQueryService.getChatUnreadMessages(authData);
    }
}