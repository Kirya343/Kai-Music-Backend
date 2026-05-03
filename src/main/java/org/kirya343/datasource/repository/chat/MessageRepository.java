package org.kirya343.datasource.repository.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.kirya343.datasource.model.chat.Message;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Получить все непрочитанные сообщения для пользователя
    List<Message> findBySenderOpenIdNotAndReadFalse(String senderOpenId);

    // Получить все непрочитанные сообщения для пользователя в конкретном разговоре
    List<Message> findByChatOpenIdAndSenderOpenIdNotAndReadFalse(String chatOpenId, String senderOpenId);

    List<Message> findByChatOpenIdOrderByTimestampAsc(String openId);

    long countByChatOpenIdAndSenderOpenIdNotAndReadFalse(String chatOpenId, String senderOpenId);

    // Новый метод: получить сообщения по ID разговора (с сортировкой по времени)
    Page<Message> findByChatOpenIdOrderByTimestampDesc(String chatOpenId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Message m
        SET m.read = true
        WHERE m.chatOpenId = :chatOpenId
        AND m.senderOpenId <> :userOpenId
        AND m.read = false
    """)
    void markMessagesAsRead(
            @Param("chatOpenId") String chatOpenId,
            @Param("userOpenId") String userOpenId
    );

    @Query("""
        SELECT m
        FROM Message m
        WHERE m.chatOpenId IN (
            SELECT cp.chat.openId
            FROM ChatParticipant cp
            WHERE cp.user.openId = :userOpenId
        )
        AND m.read = false
        AND m.senderOpenId <> :userOpenId
    """)
    List<Message> findUnreadMessagesByUserId(@Param("userOpenId") String userOpenId);

    Optional<Message> findTopByChatOpenIdOrderByIdDesc(String chatOpenId);
}


