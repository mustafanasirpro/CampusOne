package com.campusone.user.repository;

import com.campusone.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles"})
    @Query("select u from User u where u.id = :userId")
    Optional<User> findWithRolesById(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {
        "roles",
        "studentProfile",
        "studentProfile.university",
        "studentProfile.department"
    })
    @Query("select u from User u where u.id = :userId")
    Optional<User> findDetailedById(@Param("userId") UUID userId);
}
