package com.euem.server.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;
    
    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, columnDefinition = "verification_token_type")
    private TokenType type;
    
    public enum TokenType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        EMAIL_CHANGE
    }
}
