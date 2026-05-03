package org.kirya343.core.chat;

import org.kirya343.dto.chat.ChatDTO;
import org.kirya343.dto.chat.MessageDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.kirya343.datasource.model.chat.Chat;

public interface ChatCommandService {

    void sendMessage(MessageDTO messageDTO, UserAuthData authData) throws AccessDeniedException;
    void notifyChatUpdate(@NonNull ChatDTO chatDto, @NonNull String recipientOpenId);
    void markMessagesAsRead(String chatId, UserAuthData authData);
    void setPermanentChat(Chat chat);
    void acceptChatTerms(Long chatId, UserAuthData authData);
    void deleteTemporaryChats(UserAuthData authData);
}
