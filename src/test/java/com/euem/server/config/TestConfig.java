package com.euem.server.config;

import com.euem.server.service.EmailService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public EmailService emailService() {
        return mock(EmailService.class);
    }
}

