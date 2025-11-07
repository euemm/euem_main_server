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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUser().getId();
        
        UserResponse user = userService.getUserProfile(userId);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
        Authentication authentication,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUser().getId();
        
        UserResponse user = userService.updateProfile(userId, request);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/change-email")
    public ResponseEntity<MessageResponse> changeEmail(
        Authentication authentication,
        @Valid @RequestBody ChangeEmailRequest request
    ) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUser().getId();
        
        userService.changeEmail(userId, request);
        return ResponseEntity.ok(MessageResponse.success("Verification code sent to new email address"));
    }
    
    @PostMapping("/verify-new-email")
    public ResponseEntity<UserResponse> verifyNewEmail(
        Authentication authentication,
        @Valid @RequestBody VerifyEmailRequest request
    ) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUser().getId();
        
        UserResponse user = userService.verifyNewEmail(userId, request.getOtpCode());
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
        Authentication authentication,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUser().getId();
        
        userService.changePassword(userId, request);
        return ResponseEntity.ok(MessageResponse.success("Password changed successfully"));
    }
    
    @DeleteMapping("/account")
    public ResponseEntity<MessageResponse> deleteAccount(Authentication authentication) {
        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();
        UUID userId = userPrincipal.getUser().getId();
        
        userService.deleteAccount(userId);
        return ResponseEntity.ok(MessageResponse.success("Account deleted successfully"));
    }
}
