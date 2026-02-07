package com.restrictedfileshare.repository;

import com.restrictedfileshare.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);

    @Query("SELECT vt FROM VerificationToken vt WHERE vt.user.id = :userId AND vt.used = false ORDER BY vt.createdAt DESC")
    Optional<VerificationToken> findLatestByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(vt) > 0 FROM VerificationToken vt WHERE vt.user.id = :userId AND vt.createdAt > :timeLimit")
    boolean existsRecentToken(@Param("userId") Long userId, @Param("timeLimit") LocalDateTime timeLimit);

    @Modifying
    @Query("UPDATE VerificationToken vt SET vt.used = true WHERE vt.user.id = :userId")
    void invalidateUserTokens(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiryTime < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}