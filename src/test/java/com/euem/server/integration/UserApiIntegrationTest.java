package com.euem.server.integration;

import com.euem.server.entity.User;
import com.euem.server.repository.UserRepository;
import com.euem.server.security.CustomUserPrincipal;
import com.euem.server.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Order(4)
class UserApiIntegrationTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	private static final String TEST_EMAIL = "user-api-test@euem.net";
	private static final String TEST_PASSWORD = "TestPassword123!";
	private static User testUser;
	private static String authToken;
	
	@BeforeAll
	void beforeAll(@Autowired com.euem.server.repository.RoleRepository roleRepository,
	               @Autowired JwtTokenProvider jwtTokenProvider,
	               @Autowired com.euem.server.repository.VerificationTokenRepository tokenRepository) {
		System.out.println("\n" + "=".repeat(80));
		System.out.println("USER API INTEGRATION TESTS");
		System.out.println("=".repeat(80));
		
		// Clean up any existing test data
		userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> {
			tokenRepository.deleteAll(tokenRepository.findAll().stream()
				.filter(token -> token.getUser().getId().equals(user.getId()))
				.toList());
			userRepository.delete(user);
		});
		
		// Create test user
		com.euem.server.entity.Role userRole = roleRepository.findByName(com.euem.server.entity.Role.RoleName.USER)
			.orElseThrow(() -> new RuntimeException("USER role not found"));
		
		testUser = new User();
		testUser.setEmail(TEST_EMAIL);
		testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
		testUser.setFirstName("User");
		testUser.setLastName("Test");
		testUser.setIsVerified(true);
		testUser.setIsEnabled(true);
		testUser.getRoles().add(userRole);
		testUser = userRepository.save(testUser);
		
		// Generate JWT token
		CustomUserPrincipal userPrincipal = CustomUserPrincipal.create(testUser);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			userPrincipal, null, userPrincipal.getAuthorities());
		authToken = jwtTokenProvider.generateToken(authentication);
		
		System.out.println("✓ Test user created: " + TEST_EMAIL);
	}
	
	@AfterAll
	void afterAll(@Autowired com.euem.server.repository.VerificationTokenRepository tokenRepository) {
		// Clean up test data
		userRepository.findByEmail(TEST_EMAIL).ifPresent(user -> {
			tokenRepository.deleteAll(tokenRepository.findAll().stream()
				.filter(token -> token.getUser().getId().equals(user.getId()))
				.toList());
			userRepository.delete(user);
		});
		
		System.out.println("=".repeat(80));
		System.out.println("✓ User API tests completed and cleaned up");
		System.out.println("=".repeat(80) + "\n");
	}
	
	@Test
	@Order(1)
	@DisplayName("Test get user profile")
	void testGetProfile() throws Exception {
		mockMvc.perform(get("/users/profile")
				.header("Authorization", "Bearer " + authToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(testUser.getId().toString()))
				.andExpect(jsonPath("$.email").value(TEST_EMAIL))
				.andExpect(jsonPath("$.firstName").value("User"))
				.andExpect(jsonPath("$.lastName").value("Test"))
				.andExpect(jsonPath("$.isVerified").value(true))
				.andExpect(jsonPath("$.isEnabled").value(true));
		
		System.out.println("✓ Get profile successful");
	}
	
	@Test
	@Order(2)
	@DisplayName("Test get profile without authentication")
	void testGetProfileUnauthorized() throws Exception {
		mockMvc.perform(get("/users/profile"))
				.andExpect(status().isUnauthorized());
		
		System.out.println("✓ Unauthorized access correctly rejected");
	}
	
	@Test
	@Order(3)
	@DisplayName("Test update user profile")
	void testUpdateProfile() throws Exception {
		String requestBody = """
			{
				"firstName": "Updated",
				"lastName": "Name"
			}
			""";
		
		mockMvc.perform(put("/users/profile")
				.header("Authorization", "Bearer " + authToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Updated"))
				.andExpect(jsonPath("$.lastName").value("Name"))
				.andExpect(jsonPath("$.email").value(TEST_EMAIL));
		
		// Verify in database
		User user = userRepository.findByEmail(TEST_EMAIL)
			.orElseThrow(() -> new RuntimeException("Test user not found"));
		Assertions.assertEquals("Updated", user.getFirstName());
		Assertions.assertEquals("Name", user.getLastName());
		
		System.out.println("✓ Update profile successful");
	}
	
	@Test
	@Order(4)
	@DisplayName("Test change password")
	void testChangePassword() throws Exception {
		String requestBody = String.format("""
			{
				"currentPassword": "%s",
				"newPassword": "NewPassword123!"
			}
			""", TEST_PASSWORD);
		
		mockMvc.perform(put("/users/change-password")
				.header("Authorization", "Bearer " + authToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Password changed successfully"))
				.andExpect(jsonPath("$.success").value(true));
		
		// Verify password changed in database
		User user = userRepository.findByEmail(TEST_EMAIL)
			.orElseThrow(() -> new RuntimeException("Test user not found"));
		Assertions.assertTrue(passwordEncoder.matches("NewPassword123!", user.getPassword()));
		
		// Change password back for other tests
		user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
		userRepository.save(user);
		
		System.out.println("✓ Change password successful");
	}
	
	@Test
	@Order(5)
	@DisplayName("Test change password with invalid current password")
	void testChangePasswordInvalid() throws Exception {
		String requestBody = """
			{
				"currentPassword": "WrongPassword123!",
				"newPassword": "NewPassword123!"
			}
			""";
		
		mockMvc.perform(put("/users/change-password")
				.header("Authorization", "Bearer " + authToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isBadRequest());
		
		System.out.println("✓ Invalid current password correctly rejected");
	}
}

