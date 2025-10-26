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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {
    
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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setIsVerified(false);
        user.setIsEnabled(true);
        
        // Assign USER role
        Role userRole = roleRepository.findByName(Role.RoleName.USER)
            .orElseThrow(() -> new RuntimeException("USER role not found"));
        user.getRoles().add(userRole);
        
        User savedUser = userRepository.save(user);
        
        // Send verification email
        emailService.sendVerificationEmail(savedUser, VerificationToken.TokenType.EMAIL_VERIFICATION);
        
        return convertToUserResponse(savedUser);
    }
    
    public UserResponse verifyEmail(String otpCode) {
        Optional<VerificationToken> tokenOpt = verificationTokenRepository
            .findByOtpCodeAndTypeAndExpiryTimeAfter(otpCode, VerificationToken.TokenType.EMAIL_VERIFICATION, LocalDateTime.now());
        
        if (tokenOpt.isEmpty()) {
            throw new InvalidOtpException("Invalid or expired OTP code");
        }
        
        VerificationToken token = tokenOpt.get();
        User user = token.getUser();
        
        if (user.getIsVerified()) {
            throw new InvalidOtpException("Email already verified");
        }
        
        user.setIsVerified(true);
        userRepository.save(user);
        
        // Delete the used token
        verificationTokenRepository.delete(token);
        
        return convertToUserResponse(user);
    }
    
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        
        if (user.getIsVerified()) {
            throw new InvalidOtpException("Email already verified");
        }
        
        emailService.sendVerificationEmail(user, VerificationToken.TokenType.EMAIL_VERIFICATION);
    }
    
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findByIdAndIsEnabledTrue(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
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
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getNewEmail());
        }
        
        emailService.sendEmailChangeVerification(user, request.getNewEmail(), VerificationToken.TokenType.EMAIL_CHANGE);
    }
    
    public UserResponse verifyNewEmail(UUID userId, String otpCode) {
        User user = userRepository.findByIdAndIsEnabledTrue(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        Optional<VerificationToken> tokenOpt = verificationTokenRepository
            .findByOtpCodeAndTypeAndExpiryTimeAfter(otpCode, VerificationToken.TokenType.EMAIL_CHANGE, LocalDateTime.now());
        
        if (tokenOpt.isEmpty()) {
            throw new InvalidOtpException("Invalid or expired OTP code");
        }
        
        VerificationToken token = tokenOpt.get();
        if (!token.getUser().getId().equals(userId)) {
            throw new InvalidOtpException("Invalid OTP code for this user");
        }
        
        // Note: In a real implementation, you'd need to store the new email temporarily
        // For this example, we'll assume the new email is passed in the request
        // This would require additional fields in VerificationToken or a separate table
        
        verificationTokenRepository.delete(token);
        return convertToUserResponse(user);
    }
    
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdAndIsEnabledTrue(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    
    public void deleteAccount(UUID userId) {
        User user = userRepository.findByIdAndIsEnabledTrue(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Soft delete
        user.setIsEnabled(false);
        userRepository.save(user);
        
        // Delete all verification tokens for this user
        verificationTokenRepository.deleteByUserAndType(userId, VerificationToken.TokenType.EMAIL_VERIFICATION);
        verificationTokenRepository.deleteByUserAndType(userId, VerificationToken.TokenType.PASSWORD_RESET);
        verificationTokenRepository.deleteByUserAndType(userId, VerificationToken.TokenType.EMAIL_CHANGE);
    }
    
    public UserResponse getUserProfile(UUID userId) {
        User user = userRepository.findByIdAndIsEnabledTrue(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        return convertToUserResponse(user);
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
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
}
