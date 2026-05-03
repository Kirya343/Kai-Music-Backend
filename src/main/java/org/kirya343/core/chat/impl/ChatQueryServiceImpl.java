package org.kirya343.core.chat.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.kirya343.dto.auth.UserAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.kirya343.dto.chat.ChatDTO;
import org.kirya343.dto.chat.ChatDetails;
import org.kirya343.dto.chat.ChatMemberDTO;
import org.kirya343.dto.chat.ChatsLoadedEvent;
import org.kirya343.dto.chat.MessageDTO;
import org.kirya343.enums.chat.ChatType;
import org.kirya343.core.chat.ChatQueryService;
import org.kirya343.core.user.UserMappingService;
import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.repository.chat.ChatParticipantRepository;
import org.kirya343.datasource.repository.chat.ChatRepository;
import org.kirya343.datasource.repository.chat.MessageRepository;
import org.kirya343.datasource.repository.user.UserRepository;
import org.kirya343.dto.user.ShortUserDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

import org.kirya343.core.chat.ChatMappingService;
import org.kirya343.datasource.model.chat.Chat;
import org.kirya343.datasource.model.chat.Message;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatQueryServiceImpl implements ChatQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ChatQueryService.class);
    
    private final ChatRepository chatRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;

    private final EntityManager entityManager;
    private final UserMappingService userMappingService;
    private final ChatMappingService mappingService;

    private final UserRepository userRepository;

    private final ApplicationEventPublisher eventPublisher;

    public Chat getOrCreatePrivateChat(UserAuthData authData, String interlocutorOpenId) {
        Optional<Chat> existing = chatRepository.findChatBetweenUsersAndChatTypeAndTargetId(authData.openId(), interlocutorOpenId, ChatType.PRIVATE_CHAT, null);
        if (existing.isPresent()) {
            return existing.get();
        }

        User user1 = userRepository.findByOpenId(interlocutorOpenId).orElseThrow(
            () -> new EntityNotFoundException("Пользователь не найден"));

        User user2Proxy = entityManager.getReference(User.class, authData.id());
        Set<User> participants = Set.of(user1, user2Proxy);
        Chat chat = new Chat(participants, ChatType.PRIVATE_CHAT, null);
        return chatRepository.save(chat);
    }

    @Transactional
    public List<ChatDTO> getChatsDTOForUser(UserAuthData authData) {
        List<ChatDTO> chats = chatRepository.findChatsForUser(authData.openId());

        logger.debug("Chats for DTO found: " + chats.size());
        eventPublisher.publishEvent(new ChatsLoadedEvent(authData, chats));
        return chats;
    }

    @NonNull
    public List<ChatDetails> getChatDetails(Long userId, List<ChatDTO> chats) {
        List<String> chatIds = chats.stream().map(c -> c.openId()).toList();

        List<ChatMemberDTO> members = chatParticipantRepository.findMembersByChatOpenIds(chatIds);

        List<ChatDetails> chatDetails = chats.stream()
            .map(c -> {
                // участники текущего чата
                List<ShortUserDTO> chatMembers = members.stream()
                    .filter(m -> m.chatOpenId().equals(c.openId()))
                    .map(u -> new ShortUserDTO(
                        u.openId(),
                        u.name(),
                        u.avatarUrl()
                    ))
                    .toList();

                return new ChatDetails(c.openId(), chatMembers);
            }).toList();

        return Objects.requireNonNull(chatDetails);
    }

    public List<MessageDTO> getMessagesByChatId(String chatOpenId, UserAuthData authData) {
        logger.debug("Получение сообщений для разговора с ID: {}", chatOpenId);

        if (!chatParticipantRepository.existsByChatOpenIdAndUserId(chatOpenId, authData.id())) {
            throw new AccessDeniedException("That is not your chat");
        }

        // Получаем все сообщения для этого разговора
        List<Message> messages = messageRepository.findByChatOpenIdOrderByTimestampAsc(chatOpenId);

        // Преобразуем сообщения в DTO и отправляем клиенту
        List<MessageDTO> messageDtos = messages.stream()
            .map(msg -> mappingService.toDTO(msg))
            .collect(Collectors.toList());

        return messageDtos;
    }

    public long getUnreadMessageCount(String chatOpenId, UserAuthData authData) {
        // Получаем все непрочитанные сообщения для конкретного разговора и пользователя
        return messageRepository.findByChatOpenIdAndSenderOpenIdNotAndReadFalse(chatOpenId, authData.openId()).size();
    }

    public Chat getChatById(String chatOpenId) {
        if (chatOpenId == null) {
            throw new IllegalStateException("Id чата не найдено");
        }
        return chatRepository.findByOpenId(chatOpenId).orElseThrow(
            () -> new EntityNotFoundException("Чата с таким Id не существует"));
    }

    public Boolean isChatTermsAccepted(Long chatId, UserAuthData authData) {
        Boolean accepted = chatParticipantRepository.isChatTermsAccepted(authData.id(), chatId);
        if (accepted == null) {
            throw new AccessDeniedException("That is not your chat");
        }
        return accepted;
    }

    public List<ShortUserDTO> getChatInterlocutors(Long chatId, UserAuthData authData) {

        ChatType chatType = chatRepository.findTypeById(chatId);

        if (chatType == ChatType.PRIVATE_CHAT) {
            boolean isParticipant = chatParticipantRepository.existsByChatIdAndUserIdAndChatTypeIn(
                chatId, authData.id(), List.of(ChatType.PRIVATE_CHAT)
            );
            if (!isParticipant) throw new AccessDeniedException("Нет доступа к приватному чату");
        }

        List<User> interlocutors = chatParticipantRepository.findChatInterlocutorsExcludingUser(chatId, authData.id());

        return interlocutors.stream().map(user -> userMappingService.toShortDTO(user)).toList();
    }

    public List<MessageDTO> getChatUnreadMessages(UserAuthData authData) {
        List<Message> unreads = messageRepository.findUnreadMessagesByUserId(authData.openId());
        logger.debug("Найдены непрочитанные сообщения для " + authData.name() + ": " + unreads.size());
        return unreads.stream().map(m -> mappingService.toDTO(m)).toList();
    }

    public ChatDTO getChatDTO(String chatOpenId, String userOpenId) {
        Chat chat = getChatById(chatOpenId);
        return mappingService.convertToDTO(chat, userOpenId);
    }
}
