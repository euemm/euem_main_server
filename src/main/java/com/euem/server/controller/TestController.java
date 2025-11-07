package com.euem.server.controller;

import com.euem.server.entity.Role;
import com.euem.server.entity.User;
import com.euem.server.entity.VerificationToken;
import com.euem.server.repository.RoleRepository;
import com.euem.server.repository.UserRepository;
import com.euem.server.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private EmailService emailService;
    
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test user repository
            long userCount = userRepository.count();
            result.put("userCount", userCount);
            
            // Test role repository
            long roleCount = roleRepository.count();
            result.put("roleCount", roleCount);
            
            // Check if default roles exist
            boolean hasUserRole = roleRepository.findByName(Role.RoleName.USER).isPresent();
            boolean hasAdminRole = roleRepository.findByName(Role.RoleName.ADMIN).isPresent();
            
            result.put("hasUserRole", hasUserRole);
            result.put("hasAdminRole", hasAdminRole);
            result.put("status", "Database connection successful");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "Database connection failed");
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    @PostMapping("/email")
    public ResponseEntity<Map<String, Object>> testEmail(@RequestParam String email) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Create a test user
            User testUser = new User();
            testUser.setEmail(email);
            testUser.setFirstName("Test");
            testUser.setLastName("User");
            
            // Send test email
            emailService.sendVerificationEmail(testUser, VerificationToken.TokenType.EMAIL_VERIFICATION);
            
            result.put("status", "Email sent successfully");
            result.put("message", "Check your email for verification code");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "Email sending failed");
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}