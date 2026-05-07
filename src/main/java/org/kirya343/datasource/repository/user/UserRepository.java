package org.kirya343.datasource.repository.user;

import org.kirya343.datasource.model.user.User;
import org.kirya343.datasource.model.user.permission.Role;
import org.kirya343.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
    Optional<User> findByEmail(String email);
    Optional<User> findByOpenId(String openId);
    Optional<User> findByApiKey(String apiKey);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<User> findByRolesContaining(Role role);
    List<User> findByRoles_Name(String roleName);
    List<User> findByRoles_NameIn(List<String> roleNames);

    List<User> findByStatus(UserStatus status);

    boolean existsByEmail(String email);
    boolean existsByName(String name);
    boolean existsByIdAndRoles_Name(Long userId, String roleName);

    Page<User> findAllByStatusOrderByCreatedAtDesc(Pageable pageable, UserStatus type);

    int countByStatus(UserStatus status);

    @Modifying
    @Transactional
    @Query("update User u set u.name = :name where u.id = :id")
    void updateName(@Param("id") Long id, @Param("name") String name);
    
    @Query("""
        select distinct u
        from User u
        left join fetch u.settings
        left join fetch u.roles r
        left join fetch r.permissions
        where u.id = :id
    """)
    Optional<User> getFullUser(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("update User u set u.status = :status where u.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") UserStatus status);

    @Modifying
    @Transactional
    @Query(
        value = """
            insert into user_roles (user_id, roles_id)
            values (:userId, :roleId)
            """,
        nativeQuery = true
    )
    void addRoleToUser(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId
    );

    @Modifying
    @Transactional
    @Query(
        value = """
            delete from user_roles
            where user_id = :userId and roles_id = :roleId
            """,
        nativeQuery = true
    )
    void removeRoleFromUser(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId
    );

    @Modifying
    @Transactional
    @Query("update User u set u.listeningRoom.id = :roomId where u.id = :userId")
    void updateListeningRoom(@Param("userId") Long userId, @Param("roomId") Long roomId);
}
