package com.euem.server.service;

import com.euem.server.entity.User;
import com.euem.server.entity.VerificationToken;
import com.euem.server.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class EmailService {
	
	private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;
    
    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;
    
    @Value("${app.otp.length}")
    private int otpLength;
    
	public void sendVerificationEmail(User user, VerificationToken.TokenType tokenType) {
		log.info("Starting to send verification email to: {}, type: {}", user.getEmail(), tokenType);
		
		String otpCode = generateOtpCode();
		LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
		
		// Delete any existing tokens for this user and type
		log.debug("Deleting existing tokens for user: {}, type: {}", user.getId(), tokenType.name());
		verificationTokenRepository.deleteByUserAndType(user.getId(), tokenType.name());
		
		// Create new verification token
		VerificationToken token = new VerificationToken();
		token.setUser(user);
		token.setOtpCode(otpCode);
		token.setExpiryTime(expiryTime);
		token.setType(tokenType);
		verificationTokenRepository.save(token);
		log.debug("Saved verification token for user: {}", user.getId());
		
		// Send email
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(user.getEmail());
		message.setSubject(getEmailSubject(tokenType));
		message.setText(getEmailBody(otpCode, tokenType));
		
		log.info("Attempting to send email to: {} with subject: {}", user.getEmail(), message.getSubject());
		try {
			mailSender.send(message);
			log.info("Email sent successfully to: {}", user.getEmail());
		} catch (Exception e) {
			log.error("Failed to send email to: {}. Error: {}", user.getEmail(), e.getMessage(), e);
			throw e;
		}
	}
    
	public void sendEmailChangeVerification(User user, String newEmail, VerificationToken.TokenType tokenType) {
		log.info("Starting to send email change verification to: {} (old: {})", newEmail, user.getEmail());
		
		String otpCode = generateOtpCode();
		LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
		
		// Delete any existing tokens for this user and type
		log.debug("Deleting existing tokens for user: {}, type: {}", user.getId(), tokenType.name());
		verificationTokenRepository.deleteByUserAndType(user.getId(), tokenType.name());
		
		// Create new verification token
		VerificationToken token = new VerificationToken();
		token.setUser(user);
		token.setOtpCode(otpCode);
		token.setExpiryTime(expiryTime);
		token.setType(tokenType);
		verificationTokenRepository.save(token);
		log.debug("Saved verification token for user: {}", user.getId());
		
		// Send email to new address
		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(newEmail);
		message.setSubject("Verify Your New Email Address");
		message.setText(getEmailChangeBody(otpCode, user.getEmail()));
		
		log.info("Attempting to send email change verification to: {}", newEmail);
		try {
			mailSender.send(message);
			log.info("Email change verification sent successfully to: {}", newEmail);
		} catch (Exception e) {
			log.error("Failed to send email change verification to: {}. Error: {}", newEmail, e.getMessage(), e);
			throw e;
		}
	}
    
    private String generateOtpCode() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
    
    private String getEmailSubject(VerificationToken.TokenType tokenType) {
        switch (tokenType) {
            case EMAIL_VERIFICATION:
                return "Verify Your Email Address";
            case PASSWORD_RESET:
                return "Reset Your Password";
            case EMAIL_CHANGE:
                return "Verify Your New Email Address";
            default:
                return "Verification Code";
        }
    }
    
    private String getEmailBody(String otpCode, VerificationToken.TokenType tokenType) {
        switch (tokenType) {
            case EMAIL_VERIFICATION:
                return String.format(
                    "Welcome! Please use the following code to verify your email address:\n\n" +
                    "Verification Code: %s\n\n" +
                    "This code will expire in %d minutes.\n\n" +
                    "If you didn't create an account, please ignore this email.",
                    otpCode, otpExpiryMinutes
                );
            case PASSWORD_RESET:
                return String.format(
                    "You requested to reset your password. Please use the following code:\n\n" +
                    "Reset Code: %s\n\n" +
                    "This code will expire in %d minutes.\n\n" +
                    "If you didn't request this, please ignore this email.",
                    otpCode, otpExpiryMinutes
                );
            default:
                return String.format(
                    "Your verification code is: %s\n\n" +
                    "This code will expire in %d minutes.",
                    otpCode, otpExpiryMinutes
                );
        }
    }
    
    private String getEmailChangeBody(String otpCode, String oldEmail) {
        return String.format(
            "You requested to change your email address from %s.\n\n" +
            "Please use the following code to verify your new email address:\n\n" +
            "Verification Code: %s\n\n" +
            "This code will expire in %d minutes.\n\n" +
            "If you didn't request this change, please ignore this email.",
            oldEmail, otpCode, otpExpiryMinutes
        );
    }
}
