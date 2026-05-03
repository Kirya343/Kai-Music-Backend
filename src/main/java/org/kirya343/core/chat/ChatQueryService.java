package org.kirya343.core.chat;

import java.util.List;

import org.kirya343.datasource.model.chat.Chat;
import org.kirya343.dto.chat.ChatDTO;
import org.kirya343.dto.chat.ChatDetails;
import org.kirya343.dto.chat.MessageDTO;
import org.kirya343.dto.user.ShortUserDTO;
import org.kirya343.dto.auth.UserAuthData;
import org.springframework.lang.NonNull;

public interface ChatQueryService {
    
    Chat getOrCreatePrivateChat(UserAuthData authData, Long interlocutorId);

    ChatDTO getChatDTO(String chatOpenId, String userOpenId);
    List<ChatDTO> getChatsDTOForUser(UserAuthData authData);
    List<MessageDTO> getMessagesByChatId(String chatOpenId, UserAuthData authData);
    List<MessageDTO> getChatUnreadMessages(UserAuthData authData);

    long getUnreadMessageCount(String chatOpenId, UserAuthData authData);
    Chat getChatById(String chatOpenId);

    Boolean isChatTermsAccepted(Long chatId, UserAuthData authData);

    List<ShortUserDTO> getChatInterlocutors(Long chatId, UserAuthData authData);

    @NonNull
    List<ChatDetails> getChatDetails(Long userId, List<ChatDTO> chats);
}
