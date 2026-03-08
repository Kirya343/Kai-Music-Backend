package org.kirya343.datasource.repository.user.permission;

import java.util.List;

import org.kirya343.datasource.model.user.permission.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Permission findByName(String name);

    List<Permission> findAllByIdIn(List<Long> ids);

    @Modifying
    @Transactional
    @Query("""
        update Permission p
        set
            p.name = coalesce(:name, p.name),
            p.comment = coalesce(:comment, p.comment)
        where p.id = :permissionId
    """)
    void updatePermission(
            @Param("permissionId") Long permissionId,
            @Param("name") String name,
            @Param("comment") String comment
    );
}
