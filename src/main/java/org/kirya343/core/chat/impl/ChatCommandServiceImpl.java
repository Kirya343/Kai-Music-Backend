package org.kirya343.core.chat.impl;

import java.util.List;
import java.util.Objects;

import org.kirya343.dto.auth.UserAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.kirya343.dto.chat.ChatDTO;
import org.kirya343.dto.chat.MessageDTO;
import org.kirya343.enums.chat.ChatStatus;
import org.kirya343.core.chat.ChatCommandService;
import org.kirya343.core.chat.ChatMappingService;
import org.kirya343.core.chat.ChatQueryService;
import org.kirya343.datasource.model.chat.Chat;
import org.kirya343.datasource.model.chat.Message;
import org.kirya343.datasource.repository.chat.ChatParticipantRepository;
import org.kirya343.datasource.repository.chat.ChatRepository;
import org.kirya343.datasource.repository.chat.MessageRepository;
import org.kirya343.datasource.view.ChatParticipantView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatCommandServiceImpl implements ChatCommandService {

    private static final Logger logger = LoggerFactory.getLogger(ChatCommandService.class);

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatParticipantRepository chatParticipantRepository;

    private final ChatQueryService chatQueryService;
    private final ChatMappingService mappingService;
    /* private final NotificationCommandService notificationCommandService; */

    public void notifyChatUpdate(@NonNull ChatDTO chatDto, @NonNull String recipientOpenId) {

        // Отправляем обновление конкретному пользователю
        messagingTemplate.convertAndSendToUser(
            recipientOpenId,
            "/queue/chat.chats-updates",
            chatDto
        );
    }

    @Transactional
    public void sendMessage(MessageDTO dto, UserAuthData authData) {

        String chatOpenId = dto.chatOpenId();

        // 1. Проверка доступа (1 быстрый EXISTS)
        if (!chatParticipantRepository.existsByChatOpenIdAndUserId(chatOpenId, authData.id())) {
            throw new AccessDeniedException("That is not your chat");
        }

        // 2. Создание сообщения (без Chat entity)
        Message message = messageRepository.save(
            Message.create(chatOpenId, authData.openId(), dto.content())
        );

        MessageDTO msgDto = Objects.requireNonNull(
            mappingService.toDTO(message)
        );

        // 3. Помечаем чат постоянным (UPDATE)
        chatRepository.setStatus(chatOpenId, ChatStatus.ACTIVE);

        // 4. Готовим DTO чата ОДИН РАЗ
        ChatDTO chatDto = Objects.requireNonNull(
            chatQueryService.getChatDTO(chatOpenId, authData.openId())
        );

        // 5. Участники — 1 запрос
        List<ChatParticipantView> participants =
            chatParticipantRepository.findParticipantsView(chatOpenId);

        // 6. Уведомление
        /* NotificationDTO notification = new NotificationDTO(
            "Новое сообщение",
            authData.name() + ": " + message.getText(),
            "/account/messenger?chatId=" + chatId
        ); */

        // TODO переписать создание уведомления в модуль уведомлений, переделать на ивенты

        for (ChatParticipantView p : participants) {

            if (p.getUserId().equals(authData.id())) {
                continue;
            }

            String openId = Objects.requireNonNull(p.getOpenId()); 

            messagingTemplate.convertAndSendToUser(
                openId,
                "/queue/chat/messages",
                msgDto
            );

            notifyChatUpdate(chatDto, openId);

            /* notificationCommandService.saveChatNotification(
                p.getUserId(),
                notification
            ); */
        }
    }

    @Transactional
    public void markMessagesAsRead(String chatOpenId, UserAuthData authData) {
        List<Message> messages = messageRepository.findByChatOpenIdAndSenderOpenIdNotAndReadFalse(chatOpenId, authData.openId());
        for (Message m : messages) {
            m.setRead(true);
        }
        messageRepository.markMessagesAsRead(chatOpenId, authData.openId());

        ChatDTO chatDto = Objects.requireNonNull(
            chatQueryService.getChatDTO(chatOpenId, authData.openId()));
        notifyChatUpdate(chatDto, authData.openId());

        List<MessageDTO> dtos = Objects.requireNonNull(
            messages.stream().map(m -> mappingService.toDTO(m)).toList());

        messagingTemplate.convertAndSendToUser(authData.openId(), "/queue/chat/messages", dtos);
    }

    public void setPermanentChat(Chat chat) {
        chat.setStatus(ChatStatus.ACTIVE);
        chatRepository.save(chat);
    }

    @Transactional
    public void deleteChat(Chat chat) {
        
        logger.debug("Начинаем удаление чата {}", chat.getId());
        List<Message> messages = messageRepository.findByChatOpenIdOrderByTimestampAsc(chat.getOpenId());

        logger.debug("> Чистим сообщения");

        if (!messages.isEmpty()) {
            for(Message msg : messages) {
                logger.debug(">> Удаляем сообщение {}", msg.getId());
                messageRepository.delete(msg);
            }
        } else {
            logger.debug("> В чате не было сообщений");
        }

        logger.debug("Удаляем чат");
        
        chatRepository.delete(chat);
    }

    public void acceptChatTerms(Long chatId, UserAuthData authData) {
        int updated = chatParticipantRepository.acceptChatTerms(
            chatId,
            authData.id()
        );

        if (updated == 0) {
            throw new AccessDeniedException("That is not your chat");
        }
    }

    public void deleteTemporaryChats(UserAuthData authData) {
        logger.debug("Запрос на удаление временных диалогов от пользователя: {}", authData.id());

        chatRepository.deleteEmptyChatsByStatus(authData.id(), ChatStatus.TEMPORARY);
    }
}
