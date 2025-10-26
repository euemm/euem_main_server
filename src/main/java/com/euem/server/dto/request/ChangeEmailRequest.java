package com.euem.server.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeEmailRequest {
    
    @NotBlank(message = "New email is required")
    @Email(message = "New email should be valid")
    private String newEmail;
}
