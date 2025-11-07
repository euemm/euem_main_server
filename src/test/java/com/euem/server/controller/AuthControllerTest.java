package com.euem.server.controller;

import com.euem.server.config.TestConfig;
import com.euem.server.config.TestSecurityConfig;
import com.euem.server.entity.Role;
import com.euem.server.entity.User;
import com.euem.server.repository.RoleRepository;
import com.euem.server.repository.UserRepository;
import com.euem.server.repository.VerificationTokenRepository;
import com.euem.server.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestSecurityConfig.class})
@Transactional
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private Role userRole;
    
    @BeforeEach
    void setUp() {
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        
        userRole = new Role();
        userRole.setName(Role.RoleName.USER);
        userRole = roleRepository.save(userRole);
        
        Role adminRole = new Role();
        adminRole.setName(Role.RoleName.ADMIN);
        roleRepository.save(adminRole);
    }
    
    @Test
    void testRegister_Success() throws Exception {
        String requestBody = """
            {
                "email": "test@example.com",
                "password": "password123",
                "firstName": "John",
                "lastName": "Doe"
            }
            """;
        
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.isVerified").value(false))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.roles").isArray());
    }
    
    @Test
    void testRegister_InvalidEmail() throws Exception {
        String requestBody = """
            {
                "email": "invalid-email",
                "password": "password123",
                "firstName": "John",
                "lastName": "Doe"
            }
            """;
        
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testRegister_ShortPassword() throws Exception {
        String requestBody = """
            {
                "email": "test@example.com",
                "password": "short",
                "firstName": "John",
                "lastName": "Doe"
            }
            """;
        
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testRegister_DuplicateEmail() throws Exception {
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");
        existingUser.setIsVerified(false);
        existingUser.setIsEnabled(true);
        existingUser.getRoles().add(userRole);
        userRepository.save(existingUser);
        
        String requestBody = """
            {
                "email": "existing@example.com",
                "password": "password123",
                "firstName": "John",
                "lastName": "Doe"
            }
            """;
        
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict());
    }
    
    @Test
    void testLogin_Success() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setIsVerified(true);
        user.setIsEnabled(true);
        user.getRoles().add(userRole);
        userRepository.save(user);
        
        String requestBody = """
            {
                "email": "test@example.com",
                "password": "password123"
            }
            """;
        
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists())
                .andExpect(jsonPath("$.user").exists())
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }
    
    @Test
    void testLogin_InvalidCredentials() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setIsVerified(true);
        user.setIsEnabled(true);
        user.getRoles().add(userRole);
        userRepository.save(user);
        
        String requestBody = """
            {
                "email": "test@example.com",
                "password": "wrongpassword"
            }
            """;
        
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testLogin_UserNotFound() throws Exception {
        String requestBody = """
            {
                "email": "nonexistent@example.com",
                "password": "password123"
            }
            """;
        
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testVerifyEmail_Success() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setIsVerified(false);
        user.setIsEnabled(true);
        user.getRoles().add(userRole);
        user = userRepository.save(user);
        
        com.euem.server.entity.VerificationToken token = new com.euem.server.entity.VerificationToken();
        token.setUser(user);
        token.setOtpCode("123456");
        token.setExpiryTime(java.time.LocalDateTime.now().plusMinutes(15));
        token.setType(com.euem.server.entity.VerificationToken.TokenType.EMAIL_VERIFICATION);
        verificationTokenRepository.save(token);
        
        String requestBody = """
            {
                "otpCode": "123456"
            }
            """;
        
        mockMvc.perform(post("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"))
                .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    void testVerifyEmail_InvalidOtp() throws Exception {
        String requestBody = """
            {
                "otpCode": "999999"
            }
            """;
        
        mockMvc.perform(post("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testResendOtp_Success() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setIsVerified(false);
        user.setIsEnabled(true);
        user.getRoles().add(userRole);
        userRepository.save(user);
        
        mockMvc.perform(post("/auth/resend-otp")
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code sent to your email"))
                .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    void testResendOtp_UserNotFound() throws Exception {
        mockMvc.perform(post("/auth/resend-otp")
                .param("email", "nonexistent@example.com"))
                .andExpect(status().isNotFound());
    }
}

