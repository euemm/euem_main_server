package com.euem.server.controller;

import com.euem.server.config.TestConfig;
import com.euem.server.config.TestSecurityConfig;
import com.euem.server.entity.Role;
import com.euem.server.entity.User;
import com.euem.server.entity.VerificationToken;
import com.euem.server.repository.RoleRepository;
import com.euem.server.repository.UserRepository;
import com.euem.server.repository.VerificationTokenRepository;
import com.euem.server.security.CustomUserPrincipal;
import com.euem.server.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestSecurityConfig.class})
@Transactional
class UserControllerTest {
    
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
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    private Role userRole;
    private User testUser;
    private String authToken;
    
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
        
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setIsVerified(true);
        testUser.setIsEnabled(true);
        testUser.getRoles().add(userRole);
        testUser = userRepository.save(testUser);
        
        CustomUserPrincipal userPrincipal = CustomUserPrincipal.create(testUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userPrincipal, null, userPrincipal.getAuthorities());
        authToken = jwtTokenProvider.generateToken(authentication);
    }
    
    @Test
    void testGetProfile_Success() throws Exception {
        mockMvc.perform(get("/users/profile")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.isVerified").value(true))
                .andExpect(jsonPath("$.isEnabled").value(true));
    }
    
    @Test
    void testGetProfile_Unauthorized() throws Exception {
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testUpdateProfile_Success() throws Exception {
        String requestBody = """
            {
                "firstName": "Jane",
                "lastName": "Smith"
            }
            """;
        
        mockMvc.perform(put("/users/profile")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
    
    @Test
    void testUpdateProfile_InvalidFirstName() throws Exception {
        String requestBody = """
            {
                "firstName": "A",
                "lastName": "Smith"
            }
            """;
        
        mockMvc.perform(put("/users/profile")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testChangeEmail_Success() throws Exception {
        String requestBody = """
            {
                "newEmail": "newemail@example.com"
            }
            """;
        
        mockMvc.perform(put("/users/change-email")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code sent to new email address"))
                .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    void testChangeEmail_DuplicateEmail() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("existing@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setIsVerified(true);
        otherUser.setIsEnabled(true);
        otherUser.getRoles().add(userRole);
        userRepository.save(otherUser);
        
        String requestBody = """
            {
                "newEmail": "existing@example.com"
            }
            """;
        
        mockMvc.perform(put("/users/change-email")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict());
    }
    
    @Test
    void testVerifyNewEmail_Success() throws Exception {
        VerificationToken token = new VerificationToken();
        token.setUser(testUser);
        token.setOtpCode("123456");
        token.setExpiryTime(java.time.LocalDateTime.now().plusMinutes(15));
        token.setType(VerificationToken.TokenType.EMAIL_CHANGE);
        verificationTokenRepository.save(token);
        
        String requestBody = """
            {
                "otpCode": "123456"
            }
            """;
        
        mockMvc.perform(post("/users/verify-new-email")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
    
    @Test
    void testVerifyNewEmail_InvalidOtp() throws Exception {
        String requestBody = """
            {
                "otpCode": "999999"
            }
            """;
        
        mockMvc.perform(post("/users/verify-new-email")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testChangePassword_Success() throws Exception {
        String requestBody = """
            {
                "currentPassword": "password123",
                "newPassword": "newpassword123"
            }
            """;
        
        mockMvc.perform(put("/users/change-password")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"))
                .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    void testChangePassword_InvalidCurrentPassword() throws Exception {
        String requestBody = """
            {
                "currentPassword": "wrongpassword",
                "newPassword": "newpassword123"
            }
            """;
        
        mockMvc.perform(put("/users/change-password")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testChangePassword_ShortNewPassword() throws Exception {
        String requestBody = """
            {
                "currentPassword": "password123",
                "newPassword": "short"
            }
            """;
        
        mockMvc.perform(put("/users/change-password")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testDeleteAccount_Success() throws Exception {
        mockMvc.perform(delete("/users/account")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deleted successfully"))
                .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    void testDeleteAccount_Unauthorized() throws Exception {
        mockMvc.perform(delete("/users/account"))
                .andExpect(status().isUnauthorized());
    }
}

