package com.xudis.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private Boolean enabled;

    private Set<String> authorities;
}
