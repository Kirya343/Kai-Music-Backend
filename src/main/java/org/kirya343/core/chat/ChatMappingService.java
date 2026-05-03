package org.kirya343.core.chat;

import org.kirya343.datasource.model.chat.Chat;
import org.kirya343.dto.chat.ChatDTO;
import org.kirya343.dto.chat.MessageDTO;
import org.kirya343.datasource.model.chat.Message;

public interface ChatMappingService {

    ChatDTO convertToDTO(Chat chat, String userOpenId);
    
    MessageDTO toDTO(Message message);
}
