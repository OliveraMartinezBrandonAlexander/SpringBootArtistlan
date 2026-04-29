package com.example.demo.repository;

import com.example.demo.model.TwoFactorPurpose;
import com.example.demo.model.TwoFactorToken;
import com.example.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TwoFactorTokenRepository extends JpaRepository<TwoFactorToken, Integer> {

    @Query("""
            SELECT t FROM TwoFactorToken t
            WHERE t.temporaryToken = :temporaryToken
              AND t.purpose = :purpose
              AND t.used = false
            ORDER BY t.createdAt DESC
            """)
    Optional<TwoFactorToken> findLatestActiveByTemporaryTokenAndPurpose(@Param("temporaryToken") String temporaryToken,
                                                                         @Param("purpose") TwoFactorPurpose purpose);

    @Query("""
            SELECT t FROM TwoFactorToken t
            WHERE t.usuario = :usuario
              AND t.purpose = :purpose
              AND t.used = false
            ORDER BY t.createdAt DESC
            """)
    Optional<TwoFactorToken> findLatestActiveByUsuarioAndPurpose(@Param("usuario") Usuario usuario,
                                                                  @Param("purpose") TwoFactorPurpose purpose);

    @Query("""
            SELECT t FROM TwoFactorToken t
            WHERE t.temporaryToken = :temporaryToken
              AND t.purpose = :purpose
            ORDER BY t.createdAt DESC
            """)
    Optional<TwoFactorToken> findLatestByTemporaryTokenAndPurpose(@Param("temporaryToken") String temporaryToken,
                                                                   @Param("purpose") TwoFactorPurpose purpose);

    @Modifying
    @Query("""
            UPDATE TwoFactorToken t
            SET t.used = true
            WHERE t.usuario = :usuario
              AND t.purpose = :purpose
              AND t.used = false
            """)
    int invalidatePreviousTokens(@Param("usuario") Usuario usuario,
                                 @Param("purpose") TwoFactorPurpose purpose);

    @Query("""
            SELECT t FROM TwoFactorToken t
            WHERE t.usuario = :usuario
              AND t.purpose = :purpose
              AND t.used = false
              AND t.expirationTime > :now
            ORDER BY t.createdAt DESC
            """)
    List<TwoFactorToken> findNonUsedValidTokens(@Param("usuario") Usuario usuario,
                                                 @Param("purpose") TwoFactorPurpose purpose,
                                                 @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE TwoFactorToken t SET t.attempts = t.attempts + 1 WHERE t.idToken = :idToken")
    int incrementAttempts(@Param("idToken") Integer idToken);
}
