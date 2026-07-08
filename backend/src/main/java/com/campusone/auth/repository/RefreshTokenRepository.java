package com.campusone.auth.repository;

import com.campusone.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshToken token
               set token.revokedAt = :revokedAt,
                   token.updatedAt = :revokedAt,
                   token.version = token.version + 1
             where token.tokenFamily = :tokenFamily
               and token.revokedAt is null
            """)
    int revokeActiveTokenFamily(
            @Param("tokenFamily") UUID tokenFamily,
            @Param("revokedAt") java.time.Instant revokedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update RefreshToken token
               set token.revokedAt = :revokedAt,
                   token.updatedAt = :revokedAt,
                   token.version = token.version + 1
             where token.user.id = :userId
               and token.revokedAt is null
            """)
    int revokeActiveTokensForUser(
            @Param("userId") UUID userId,
            @Param("revokedAt") java.time.Instant revokedAt);

    @Modifying
    @Query("delete from RefreshToken token where token.expiresAt <= :cutoff")
    int deleteExpiredTokens(@Param("cutoff") java.time.Instant cutoff);
}
