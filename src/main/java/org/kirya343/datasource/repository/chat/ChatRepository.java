package org.kirya343.datasource.repository.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.kirya343.datasource.model.chat.Chat;
import org.kirya343.dto.chat.ChatDTO;
import org.kirya343.enums.chat.ChatStatus;
import org.kirya343.enums.chat.ChatType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByOpenId(String openId);

    @Query("""
        SELECT c
        FROM Chat c
        WHERE c.chatType = :chatType
        AND (
                (:targetId IS NULL AND c.targetId IS NULL)
                OR c.targetId = :targetId
        )
        AND EXISTS (
                SELECT 1 FROM ChatParticipant p1
                WHERE p1.chat.id = c.id AND p1.user.id = :userId1
        )
        AND EXISTS (
                SELECT 1 FROM ChatParticipant p2
                WHERE p2.chat.id = c.id AND p2.user.id = :userId2
        )
    """)
    Optional<Chat> findChatBetweenUsersAndChatTypeAndTargetId(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            @Param("chatType") ChatType chatType,
            @Param("targetId") Long targetId);

    @Query("""
        SELECT c
        FROM Chat c
        WHERE c.chatType = :chatType
        AND c.targetId = :targetId
    """)
    List<Chat> findAllByTarget(
            @Param("chatType") ChatType chatType,
            @Param("targetId") Long targetId);

    @Query("""
        SELECT c
        FROM Chat c
        WHERE c.chatType = :chatType
        AND c.targetId = :targetId
    """)
    Optional<Chat> findChatByTargetId(
            @Param("chatType") ChatType chatType,
            @Param("targetId") Long targetId);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM Chat c
        WHERE c.status = :status
        AND EXISTS (
            SELECT 1 FROM c.participants p
            WHERE p.id = :userId
        )
        AND NOT EXISTS (
            SELECT 1 FROM Message m
            WHERE m.chatOpenId = c.openId
        )
    """)
    int deleteEmptyChatsByStatus(
        @Param("userId") Long userId,
        @Param("status") ChatStatus status
    );

    @Modifying
    @Transactional
    @Query("UPDATE Chat c SET c.status = :status WHERE c.openId = :chatOpenId")
    void setStatus(@Param("chatOpenId") String chatOpenId, @Param("status") ChatStatus status);

    @Query("SELECT c.chatType FROM Chat c WHERE c.id = :chatId")
    ChatType findTypeById(@Param("chatId") Long chatId);

    @Query("SELECT cp.chat FROM ChatParticipant cp WHERE cp.user.id = :userId")
    List<Chat> findChatsByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT new org.kirya343.dto.chat.ChatDTO(
            c.openId,
            COALESCE(SUM(CASE WHEN m.read = false AND m.senderOpenId <> :userOpenId THEN 1 ELSE 0 END), 0),
            c.status,
            c.chatType,
            new org.kirya343.dto.chat.MessageDTO(
                COALESCE(lm.openId, ''),
                COALESCE(lm.content, ''),
                lm.timestamp,
                COALESCE(lm.senderOpenId, ''),
                c.openId,
                COALESCE(lm.read, false)
            )
        )
        FROM Chat c
        LEFT JOIN Message m ON m.chatOpenId = c.openId
        LEFT JOIN Message lm ON lm.id = (
            SELECT MAX(m2.id)
            FROM Message m2
            WHERE m2.chatOpenId = c.openId
        )
        WHERE EXISTS (
            SELECT 1
            FROM ChatParticipant cp
            WHERE cp.chat = c AND cp.user.openId = :userOpenId
        )
        GROUP BY c.id, lm.content, lm.timestamp, c.status, c.chatType, c.targetId
        ORDER BY lm.timestamp DESC
    """)
    List<ChatDTO> findChatsForUser(@Param("userOpenId") String userOpenId);
}
