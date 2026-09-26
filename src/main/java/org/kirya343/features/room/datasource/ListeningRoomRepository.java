package org.kirya343.features.room.datasource;

import java.util.List;
import java.util.Optional;

import org.kirya343.features.room.dto.ShortListeningRoomDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListeningRoomRepository extends JpaRepository<ListeningRoom, Long> {
    
    @Query("""
        SELECT DISTINCT r
        FROM ListeningRoom r
        LEFT JOIN FETCH r.owner
        LEFT JOIN FETCH r.members m
        LEFT JOIN FETCH r.playlist p
        LEFT JOIN FETCH p.queue q
        LEFT JOIN FETCH q.audio
        WHERE m.id = :userId
        """)
    Optional<ListeningRoom> findRoomByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT r FROM ListeningRoom r
        LEFT JOIN FETCH r.owner
        WHERE r.id = :roomId
    """)
    Optional<ListeningRoom> findFullRoomById(@Param("roomId") Long roomId);

    boolean existsByOwnerId(Long ownerId);
    void deleteByOwnerId(Long ownerId);

    List<ListeningRoom> findAllByOwnerId(Long ownerId);
    Optional<ListeningRoom> findByCode(String code);

    @Query("""
        SELECT new org.kirya343.features.room.dto.ShortListeningRoomDTO(
            r.id,
            COALESCE(r.title, CONCAT(r.owner.name, '''s room')),
            r.owner.id,
            r.code,
            SIZE(r.members)
        )
        FROM ListeningRoom r
    """)
    List<ShortListeningRoomDTO> findAllShortDTOs();
}
