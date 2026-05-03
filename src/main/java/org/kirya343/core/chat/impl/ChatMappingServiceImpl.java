package org.kirya343.core.chat.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.kirya343.dto.chat.ChatDTO;
import org.kirya343.dto.chat.MessageDTO;
import org.kirya343.enums.chat.ChatStatus;
import org.kirya343.enums.chat.ChatType;
import org.kirya343.core.chat.ChatMappingService;
import org.kirya343.datasource.model.chat.Chat;
import org.kirya343.datasource.model.chat.Message;
import org.kirya343.datasource.repository.chat.MessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMappingServiceImpl implements ChatMappingService {
 
    private static final Logger logger = LoggerFactory.getLogger(ChatMappingService.class);

    private final MessageRepository messageRepository;

    public ChatDTO convertToDTO(Chat chat, String userOpenId) {
        logger.debug("Конвертация в дто начата разговора: " + chat.getId());

        ChatStatus status = chat.getStatus();
        ChatType type = chat.getChatType();

        logger.debug("Определяем, есть ли новые сообщения");

        long unreadcount = messageRepository.countByChatOpenIdAndSenderOpenIdNotAndReadFalse(chat.getOpenId(), userOpenId);

        logger.debug("Обработка последнего сообщения");
        // Обработка последнего сообщения
        Optional<Message> lastMessage = messageRepository.findTopByChatOpenIdOrderByIdDesc(chat.getOpenId());
        MessageDTO lastMessageDto = null;

        if (lastMessage.isPresent()) {
            Message existing = lastMessage.get();
            lastMessageDto = new MessageDTO(
                existing.getOpenId(), 
                existing.getContent(), 
                existing.getTimestamp(), 
                null, 
                chat.getOpenId(), 
                false
            );
        }

        ChatDTO dto = new ChatDTO(
            chat.getOpenId(),
            chat.getName(),
            unreadcount,
            status,
            type,
            lastMessageDto
        );

        logger.debug("Конвертация закончена");

        return dto;
    }

    // Кастомные параметры которые сделаны для того чтобы можно было указать 
    // их сразу если они имеются в методе, и тем самым ускорить загрузку
    public MessageDTO toDTO(Message message) {
        return new MessageDTO(
            message.getOpenId(),
            message.getContent(),
            message.getTimestamp(),
            message.getSenderOpenId(),
            message.getChatOpenId(),
            message.isRead()
        );
    }
}
