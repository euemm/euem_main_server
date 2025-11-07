package com.euem.server.integration;

import com.euem.server.entity.User;
import com.euem.server.entity.VerificationToken;
import com.euem.server.repository.RoleRepository;
import com.euem.server.repository.UserRepository;
import com.euem.server.repository.VerificationTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthApiIntegrationTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private VerificationTokenRepository verificationTokenRepository;
	
	private static final String TEST_EMAIL = "api-test@euem.net";
	private static final String TEST_PASSWORD = "TestPassword123!";
	private static final String TEST_FIRST_NAME = "API";
	private static final String TEST_LAST_NAME = "Test";
	
	@BeforeAll
	static void beforeAll(@Autowired UserRepository userRepository, 
	                      @Autowired VerificationTokenRepository tokenRepository) {
		// Clean up any existing test data
		userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> {
			tokenRepository.deleteAll(tokenRepository.findAll().stream()
				.filter(token -> token.getUser().getId().equals(user.getId()))
				.toList());
			userRepository.delete(user);
		});
	}
	
	@AfterAll
	static void afterAll(@Autowired UserRepository userRepository,
	                     @Autowired VerificationTokenRepository tokenRepository) {
		// Clean up test data after all tests
		userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> {
			tokenRepository.deleteAll(tokenRepository.findAll().stream()
				.filter(token -> token.getUser().getId().equals(user.getId()))
				.toList());
			userRepository.delete(user);
		});
	}
	
	@Test
	@Order(1)
	@DisplayName("Test user registration with real SMTP")
	void testRegister() throws Exception {
		String requestBody = String.format("""
			{
				"email": "%s",
				"password": "%s",
				"firstName": "%s",
				"lastName": "%s"
			}
			""", TEST_EMAIL, TEST_PASSWORD, TEST_FIRST_NAME, TEST_LAST_NAME);
		
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.email").value(TEST_EMAIL))
				.andExpect(jsonPath("$.firstName").value(TEST_FIRST_NAME))
				.andExpect(jsonPath("$.lastName").value(TEST_LAST_NAME))
				.andExpect(jsonPath("$.isVerified").value(false))
				.andExpect(jsonPath("$.isEnabled").value(true))
				.andExpect(jsonPath("$.roles").isArray());
		
		// Verify user exists in database
		Optional<User> user = userRepository.findByEmail(TEST_EMAIL);
		Assertions.assertTrue(user.isPresent(), "User should be created in database");
		Assertions.assertFalse(user.get().getIsVerified(), "User should not be verified yet");
		
		// Wait for async email sending
		Thread.sleep(500);
		
		System.out.println("✓ Registration successful - Email sent to: " + TEST_EMAIL);
		System.out.println("✓ User created in database");
	}
	
	@Test
	@Order(2)
	@DisplayName("Test duplicate email registration")
	void testRegisterDuplicateEmail() throws Exception {
		String requestBody = String.format("""
			{
				"email": "%s",
				"password": "%s",
				"firstName": "Duplicate",
				"lastName": "User"
			}
			""", TEST_EMAIL, TEST_PASSWORD);
		
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isConflict());
		
		System.out.println("✓ Duplicate email correctly rejected");
	}
	
	@Test
	@Order(3)
	@DisplayName("Test email verification with OTP from database")
	void testVerifyEmail() throws Exception {
		// Get the OTP code from database
		User user = userRepository.findByEmail(TEST_EMAIL)
			.orElseThrow(() -> new RuntimeException("Test user not found"));
		
		Optional<VerificationToken> tokenOpt = verificationTokenRepository
			.findAll().stream()
			.filter(t -> t.getUser().getId().equals(user.getId()) && 
			            t.getType() == VerificationToken.TokenType.EMAIL_VERIFICATION)
			.findFirst();
		
		Assertions.assertTrue(tokenOpt.isPresent(), "Verification token should exist");
		String otpCode = tokenOpt.get().getOtpCode();
		
		String requestBody = String.format("""
			{
				"otpCode": "%s"
			}
			""", otpCode);
		
		mockMvc.perform(post("/auth/verify-email")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Email verified successfully"))
				.andExpect(jsonPath("$.success").value(true));
		
		// Verify user is now verified in database
		User verifiedUser = userRepository.findByEmail(TEST_EMAIL)
			.orElseThrow(() -> new RuntimeException("Test user not found"));
		Assertions.assertTrue(verifiedUser.getIsVerified(), "User should be verified");
		
		System.out.println("✓ Email verification successful with OTP: " + otpCode);
	}
	
	@Test
	@Order(4)
	@DisplayName("Test resend OTP")
	void testResendOtp() throws Exception {
		mockMvc.perform(post("/auth/resend-otp")
				.param("email", TEST_EMAIL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Verification code sent to your email"))
				.andExpect(jsonPath("$.success").value(true));
		
		System.out.println("✓ Resend OTP successful - New email sent to: " + TEST_EMAIL);
	}
	
	@Test
	@Order(5)
	@DisplayName("Test login with real credentials")
	void testLogin() throws Exception {
		String requestBody = String.format("""
			{
				"email": "%s",
				"password": "%s"
			}
			""", TEST_EMAIL, TEST_PASSWORD);
		
		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").exists())
				.andExpect(jsonPath("$.user").exists())
				.andExpect(jsonPath("$.user.email").value(TEST_EMAIL));
		
		System.out.println("✓ Login successful");
		System.out.println("✓ JWT token generated");
	}
	
	@Test
	@Order(6)
	@DisplayName("Test login with invalid credentials")
	void testLoginInvalidCredentials() throws Exception {
		String requestBody = String.format("""
			{
				"email": "%s",
				"password": "WrongPassword123!"
			}
			""", TEST_EMAIL);
		
		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isUnauthorized());
		
		System.out.println("✓ Invalid credentials correctly rejected");
	}
}

