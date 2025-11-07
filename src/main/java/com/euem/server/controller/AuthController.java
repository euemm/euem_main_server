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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
	
	private static final Logger log = LoggerFactory.getLogger(AuthController.class);
	
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
		log.info("Register request received for email: {}", request.getEmail());
		try {
			UserResponse user = userService.register(request);
			log.info("Registration completed for user id: {}", user.getId());
			return ResponseEntity.ok(user);
		} catch (Exception ex) {
			log.error("Registration failed for email {}: {}", request.getEmail(), ex.getMessage(), ex);
			throw ex;
		}
	}
	
	@PostMapping("/verify-email")
	public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		log.info("Email verification requested for OTP: {}", request.getOtpCode());
		try {
			userService.verifyEmail(request.getOtpCode());
			log.info("Email verification succeeded for OTP: {}", request.getOtpCode());
			return ResponseEntity.ok(MessageResponse.success("Email verified successfully"));
		} catch (Exception ex) {
			log.error("Email verification failed for OTP {}: {}", request.getOtpCode(), ex.getMessage(), ex);
			throw ex;
		}
	}
	
	@PostMapping("/resend-otp")
	public ResponseEntity<MessageResponse> resendOtp(@RequestParam String email) {
		log.info("Resend OTP requested for email: {}", email);
		try {
			userService.resendVerificationEmail(email);
			log.info("Resend OTP succeeded for email: {}", email);
			return ResponseEntity.ok(MessageResponse.success("Verification code sent to your email"));
		} catch (Exception ex) {
			log.error("Resend OTP failed for email {}: {}", email, ex.getMessage(), ex);
			throw ex;
		}
	}
	
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		log.info("Login attempt for email: {}", request.getEmail());
		try {
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
			
			log.info("Login successful for user id: {}", user.getId());
			return ResponseEntity.ok(authResponse);
		} catch (Exception ex) {
			log.error("Login failed for email {}: {}", request.getEmail(), ex.getMessage(), ex);
			throw ex;
		}
	}
}
