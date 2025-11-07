package com.euem.server.repository;

import com.euem.server.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    
    Optional<VerificationToken> findByOtpCodeAndTypeAndExpiryTimeAfter(
        String otpCode, 
        VerificationToken.TokenType type, 
        LocalDateTime now
    );
    
    Optional<VerificationToken> findByUserAndTypeAndExpiryTimeAfter(
        com.euem.server.entity.User user, 
        VerificationToken.TokenType type, 
        LocalDateTime now
    );
    
	@Modifying
	@Query(value = "DELETE FROM verification_tokens WHERE user_id = :userId AND CAST(type AS TEXT) = :type", nativeQuery = true)
	void deleteByUserAndType(@Param("userId") UUID userId, @Param("type") String type);
    
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiryTime < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
