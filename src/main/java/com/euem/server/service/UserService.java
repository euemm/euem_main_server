package com.euem.server.service;

import com.euem.server.dto.request.ChangeEmailRequest;
import com.euem.server.dto.request.ChangePasswordRequest;
import com.euem.server.dto.request.RegisterRequest;
import com.euem.server.dto.request.UpdateProfileRequest;
import com.euem.server.dto.response.UserResponse;
import com.euem.server.entity.Role;
import com.euem.server.entity.User;
import com.euem.server.entity.VerificationToken;
import com.euem.server.exception.*;
import com.euem.server.repository.RoleRepository;
import com.euem.server.repository.UserRepository;
import com.euem.server.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {
    
	private static final Logger log = LoggerFactory.getLogger(UserService.class);
	
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public UserResponse register(RegisterRequest request) {
		log.info("Attempting to register user with email: {}", request.getEmail());
		
		Optional<User> existingOpt = userRepository.findByEmail(request.getEmail());
		User targetUser;
		if (existingOpt.isPresent()) {
			User existingUser = existingOpt.get();
			if (Boolean.TRUE.equals(existingUser.getIsEnabled())) {
				log.warn("Registration blocked because email already exists and is active: {}", request.getEmail());
				throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
			}
			
			log.info("Reactivating disabled account for email: {}", request.getEmail());
			existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
			existingUser.setFirstName(request.getFirstName());
			existingUser.setLastName(request.getLastName());
			existingUser.setIsVerified(false);
			existingUser.setIsEnabled(true);
			ensureUserRole(existingUser, request.getEmail());
			targetUser = existingUser;
		} else {
			User newUser = new User();
			newUser.setEmail(request.getEmail());
			newUser.setPassword(passwordEncoder.encode(request.getPassword()));
			newUser.setFirstName(request.getFirstName());
			newUser.setLastName(request.getLastName());
			newUser.setIsVerified(false);
			newUser.setIsEnabled(true);
			ensureUserRole(newUser, request.getEmail());
			targetUser = newUser;
		}
		
		User savedUser = userRepository.save(targetUser);
		
		log.info("User persisted with id: {}. Sending verification email.", savedUser.getId());
		// Send verification email
		emailService.sendVerificationEmail(savedUser, VerificationToken.TokenType.EMAIL_VERIFICATION);
		
		return convertToUserResponse(savedUser);
    }
    
    public UserResponse verifyEmail(String otpCode) {
		log.info("Verifying email with OTP: {}", otpCode);
		Optional<VerificationToken> tokenOpt = verificationTokenRepository
			.findByOtpCodeAndTypeAndExpiryTimeAfter(otpCode, VerificationToken.TokenType.EMAIL_VERIFICATION, LocalDateTime.now());
		
		if (tokenOpt.isEmpty()) {
			log.warn("Invalid or expired OTP during verification: {}", otpCode);
			throw new InvalidOtpException("Invalid or expired OTP code");
		}
		
		VerificationToken token = tokenOpt.get();
		User user = token.getUser();
		
		if (user.getIsVerified()) {
			log.warn("Email verification requested for already verified user id: {}", user.getId());
			throw new InvalidOtpException("Email already verified");
		}
		
		user.setIsVerified(true);
		userRepository.save(user);
		
		// Delete the used token
		verificationTokenRepository.delete(token);
		
		log.info("Email verified successfully for user id: {}", user.getId());
		return convertToUserResponse(user);
    }
    
    public void resendVerificationEmail(String email) {
		log.info("Resending verification email for: {}", email);
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> {
				log.warn("Resend OTP requested for non-existent email: {}", email);
				return new UserNotFoundException("User not found with email: " + email);
			});
		
		if (user.getIsVerified()) {
			log.warn("Resend OTP requested for already verified user id: {}", user.getId());
			throw new InvalidOtpException("Email already verified");
		}
		
		emailService.sendVerificationEmail(user, VerificationToken.TokenType.EMAIL_VERIFICATION);
		log.info("Verification email re-sent for user id: {}", user.getId());
    }
    
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
		User user = userRepository.findByIdAndIsEnabledTrue(userId)
			.orElseThrow(() -> {
				log.warn("Update profile failed because user not found: {}", userId);
				return new UserNotFoundException("User not found");
			});
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        
        User updatedUser = userRepository.save(user);
        return convertToUserResponse(updatedUser);
    }
    
    public void changeEmail(UUID userId, ChangeEmailRequest request) {
		User user = userRepository.findByIdAndIsEnabledTrue(userId)
			.orElseThrow(() -> {
				log.warn("Change email failed because user not found: {}", userId);
				return new UserNotFoundException("User not found");
			});
        
		if (userRepository.existsByEmail(request.getNewEmail())) {
			log.warn("Change email rejected because new email already exists: {}", request.getNewEmail());
			throw new UserAlreadyExistsException("Email already exists: " + request.getNewEmail());
		}
		
		emailService.sendEmailChangeVerification(user, request.getNewEmail(), VerificationToken.TokenType.EMAIL_CHANGE);
		log.info("Email change verification sent for user id: {}", userId);
    }
    
    public UserResponse verifyNewEmail(UUID userId, String otpCode) {
		User user = userRepository.findByIdAndIsEnabledTrue(userId)
			.orElseThrow(() -> {
				log.warn("Verify new email failed because user not found: {}", userId);
				return new UserNotFoundException("User not found");
			});
        
		Optional<VerificationToken> tokenOpt = verificationTokenRepository
			.findByOtpCodeAndTypeAndExpiryTimeAfter(otpCode, VerificationToken.TokenType.EMAIL_CHANGE, LocalDateTime.now());
		
		if (tokenOpt.isEmpty()) {
			log.warn("Invalid or expired OTP during email change for user id {}: {}", userId, otpCode);
			throw new InvalidOtpException("Invalid or expired OTP code");
		}
		
		VerificationToken token = tokenOpt.get();
		if (!token.getUser().getId().equals(userId)) {
			log.warn("OTP {} does not belong to user id {}", otpCode, userId);
			throw new InvalidOtpException("Invalid OTP code for this user");
		}
        
        // Note: In a real implementation, you'd need to store the new email temporarily
        // For this example, we'll assume the new email is passed in the request
        // This would require additional fields in VerificationToken or a separate table
        
		verificationTokenRepository.delete(token);
		log.info("New email verified for user id: {}", userId);
		return convertToUserResponse(user);
    }
    
    public void changePassword(UUID userId, ChangePasswordRequest request) {
		User user = userRepository.findByIdAndIsEnabledTrue(userId)
			.orElseThrow(() -> {
				log.warn("Change password failed because user not found: {}", userId);
				return new UserNotFoundException("User not found");
			});
        
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			log.warn("Change password rejected due to invalid current password for user id: {}", userId);
			throw new InvalidPasswordException("Current password is incorrect");
		}
		
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
		log.info("Password updated for user id: {}", userId);
    }
    
    public void deleteAccount(UUID userId) {
		User user = userRepository.findByIdAndIsEnabledTrue(userId)
			.orElseThrow(() -> {
				log.warn("Delete account failed because user not found: {}", userId);
				return new UserNotFoundException("User not found");
			});
        
        // Soft delete
        user.setIsEnabled(false);
        userRepository.save(user);
        
		// Delete all verification tokens for this user
		verificationTokenRepository.deleteByUserAndType(userId, VerificationToken.TokenType.EMAIL_VERIFICATION.name());
		verificationTokenRepository.deleteByUserAndType(userId, VerificationToken.TokenType.PASSWORD_RESET.name());
		verificationTokenRepository.deleteByUserAndType(userId, VerificationToken.TokenType.EMAIL_CHANGE.name());
		log.info("Account soft-deleted and tokens cleared for user id: {}", userId);
    }
    
    public UserResponse getUserProfile(UUID userId) {
		User user = userRepository.findByIdAndIsEnabledTrue(userId)
			.orElseThrow(() -> {
				log.warn("Get user profile failed because user not found: {}", userId);
				return new UserNotFoundException("User not found");
			});
        
		return convertToUserResponse(user);
    }
    
    public User findByEmail(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() -> {
				log.warn("Find by email failed because user not found: {}", email);
				return new UserNotFoundException("User not found with email: " + email);
			});
    }
    
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setIsVerified(user.getIsVerified());
        response.setIsEnabled(user.getIsEnabled());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        Set<String> roleNames = user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet());
        response.setRoles(roleNames);
        
        return response;
    }
	
	private void ensureUserRole(User user, String emailForLog) {
		Role userRole = roleRepository.findByName(Role.RoleName.USER)
			.orElseThrow(() -> {
				log.error("USER role missing while processing email: {}", emailForLog);
				return new RuntimeException("USER role not found");
			});
		if (user.getRoles() == null) {
			user.setRoles(new HashSet<>());
		}
		user.getRoles().add(userRole);
	}
}
