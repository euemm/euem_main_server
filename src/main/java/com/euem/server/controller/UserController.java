package com.euem.server.controller;

import com.euem.server.dto.request.ChangeEmailRequest;
import com.euem.server.dto.request.ChangePasswordRequest;
import com.euem.server.dto.request.UpdateProfileRequest;
import com.euem.server.dto.request.VerifyEmailRequest;
import com.euem.server.dto.response.MessageResponse;
import com.euem.server.dto.response.UserResponse;
import com.euem.server.security.CustomUserPrincipal;
import com.euem.server.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {
	
	private static final Logger log = LoggerFactory.getLogger(UserController.class);
	
	@Autowired
	private UserService userService;
	
	@GetMapping("/profile")
	public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
		CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
		UUID userId = userPrincipal.getUser().getId();
		
		log.info("Profile request for user id: {}", userId);
		try {
			UserResponse user = userService.getUserProfile(userId);
			log.info("Profile response ready for user id: {}", userId);
			return ResponseEntity.ok(user);
		} catch (Exception ex) {
			log.error("Failed to fetch profile for user id {}: {}", userId, ex.getMessage(), ex);
			throw ex;
		}
	}
	
	@PutMapping("/profile")
	public ResponseEntity<UserResponse> updateProfile(
		Authentication authentication,
		@Valid @RequestBody UpdateProfileRequest request
	) {
		CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
		UUID userId = userPrincipal.getUser().getId();
		
		log.info("Update profile request for user id: {}", userId);
		try {
			UserResponse user = userService.updateProfile(userId, request);
			log.info("Profile updated for user id: {}", userId);
			return ResponseEntity.ok(user);
		} catch (Exception ex) {
			log.error("Profile update failed for user id {}: {}", userId, ex.getMessage(), ex);
			throw ex;
		}
	}
	
	@PutMapping("/change-email")
	public ResponseEntity<MessageResponse> changeEmail(
		Authentication authentication,
		@Valid @RequestBody ChangeEmailRequest request
	) {
		CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
		UUID userId = userPrincipal.getUser().getId();
		
		log.info("Change email requested for user id: {}", userId);
		try {
			userService.changeEmail(userId, request);
			log.info("Change email initiated for user id: {}", userId);
			return ResponseEntity.ok(MessageResponse.success("Verification code sent to new email address"));
		} catch (Exception ex) {
			log.error("Change email failed for user id {}: {}", userId, ex.getMessage(), ex);
			throw ex;
		}
	}
	
	@PostMapping("/verify-new-email")
	public ResponseEntity<UserResponse> verifyNewEmail(
		Authentication authentication,
		@Valid @RequestBody VerifyEmailRequest request
	) {
		CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
		UUID userId = userPrincipal.getUser().getId();
		
		log.info("Verify new email requested for user id: {}", userId);
		try {
			UserResponse user = userService.verifyNewEmail(userId, request.getOtpCode());
			log.info("New email verified for user id: {}", userId);
			return ResponseEntity.ok(user);
		} catch (Exception ex) {
			log.error("Verify new email failed for user id {}: {}", userId, ex.getMessage(), ex);
			throw ex;
		}
	}
	
	@PutMapping("/change-password")
	public ResponseEntity<MessageResponse> changePassword(
		Authentication authentication,
		@Valid @RequestBody ChangePasswordRequest request
	) {
		CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
		UUID userId = userPrincipal.getUser().getId();
		
		log.info("Change password requested for user id: {}", userId);
		try {
			userService.changePassword(userId, request);
			log.info("Password changed for user id: {}", userId);
			return ResponseEntity.ok(MessageResponse.success("Password changed successfully"));
		} catch (Exception ex) {
			log.error("Change password failed for user id {}: {}", userId, ex.getMessage(), ex);
			throw ex;
		}
	}
	
	@DeleteMapping("/account")
	public ResponseEntity<MessageResponse> deleteAccount(Authentication authentication) {
		CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
		UUID userId = userPrincipal.getUser().getId();
		
		log.info("Delete account requested for user id: {}", userId);
		try {
			userService.deleteAccount(userId);
			log.info("Account deleted for user id: {}", userId);
			return ResponseEntity.ok(MessageResponse.success("Account deleted successfully"));
		} catch (Exception ex) {
			log.error("Account deletion failed for user id {}: {}", userId, ex.getMessage(), ex);
			throw ex;
		}
	}
}
