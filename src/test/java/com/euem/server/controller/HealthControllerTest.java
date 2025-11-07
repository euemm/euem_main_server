package com.euem.server.controller;

import com.euem.server.config.TestConfig;
import com.euem.server.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {com.euem.server.EuemMainServerApplication.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestConfig.class, TestSecurityConfig.class})
class HealthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testHealthCheck_Success() throws Exception {
        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}

