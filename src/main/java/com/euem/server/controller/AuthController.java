package com.euem.server.controller;

import com.euem.server.dto.request.LoginRequest;
import com.euem.server.dto.request.RegisterRequest;
import com.euem.server.dto.request.VerifyEmailRequest;
import com.euem.server.dto.response.AuthResponse;
import com.euem.server.dto.response.MessageResponse;
import com.euem.server.dto.response.UserResponse;
import com.euem.server.security.JwtTokenProvider;
import com.euem.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;
    
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        userService.verifyEmail(request.getOtpCode());
        return ResponseEntity.ok(MessageResponse.success("Email verified successfully"));
    }
    
    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(@RequestParam String email) {
        userService.resendVerificationEmail(email);
        return ResponseEntity.ok(MessageResponse.success("Verification code sent to your email"));
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String token = jwtTokenProvider.generateToken(authentication);
        UserResponse user = userService.getUserProfile(
            userService.findByEmail(request.getEmail()).getId()
        );
        
        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(token);
        authResponse.setExpiresIn(jwtExpiration);
        authResponse.setUser(user);
        
        return ResponseEntity.ok(authResponse);
    }
}
