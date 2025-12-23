package com.xudis.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xudis.auth.controller.AuthController;
import com.xudis.auth.config.AuthorizationServerConfig;
import com.xudis.auth.config.RedisConfig;
import com.xudis.auth.config.RegisteredClientConfig;
import com.xudis.auth.dto.CreateUserRequest;
import com.xudis.auth.dto.UpdateUserRequest;
import com.xudis.auth.dto.UserResponse;
import com.xudis.auth.service.AuthService;
import com.xudis.auth.service.JwtTokenService;
import com.xudis.auth.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        }
)
@AutoConfigureMockMvc
@ComponentScan(
        basePackages = "com.xudis.auth",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        RegisteredClientConfig.class,
                        AuthorizationServerConfig.class,
                        RedisConfig.class,
                        AuthService.class,
                        JwtTokenService.class,
                        AuthController.class
                }
        )
)
@Transactional
class UserManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserManagementService userManagementService;

    @Test
    void createUser_Success() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "testuser",
                "password123",
                true,
                Set.of("ROLE_USER")
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void createUser_DuplicateUsername_Returns409() throws Exception {
        // Create first user
        CreateUserRequest request = new CreateUserRequest(
                "duplicateuser",
                "password123",
                true,
                Set.of("ROLE_USER")
        );
        userManagementService.createUser(request);

        // Try to create with same username
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("already exists")));
    }

    @Test
    void createUser_DefaultValues() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "defaultuser",
                "password123",
                null,
                null
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("defaultuser"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")));
    }

    @Test
    void createUser_InvalidRequest_Returns400() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "",  // Empty username
                "pwd",  // Too short password
                true,
                null
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_Password() throws Exception {
        // Create user first
        CreateUserRequest createRequest = new CreateUserRequest(
                "updateuser",
                "oldpassword",
                true,
                Set.of("ROLE_USER")
        );
        userManagementService.createUser(createRequest);

        // Update password
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "newpassword123",
                null,
                null
        );

        mockMvc.perform(put("/api/users/updateuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateuser"));
    }

    @Test
    void updateUser_Enabled() throws Exception {
        // Create user first
        CreateUserRequest createRequest = new CreateUserRequest(
                "disableuser",
                "password123",
                true,
                Set.of("ROLE_USER")
        );
        userManagementService.createUser(createRequest);

        // Disable user
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                null,
                false,
                null
        );

        mockMvc.perform(put("/api/users/disableuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("disableuser"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void updateUser_Authorities() throws Exception {
        // Create user first
        CreateUserRequest createRequest = new CreateUserRequest(
                "roleuser",
                "password123",
                true,
                Set.of("ROLE_USER")
        );
        userManagementService.createUser(createRequest);

        // Update authorities
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                null,
                null,
                Set.of("ROLE_USER", "ROLE_ADMIN")
        );

        mockMvc.perform(put("/api/users/roleuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("roleuser"))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_ADMIN")));
    }

    @Test
    void updateUser_NotFound_Returns404() throws Exception {
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "newpassword",
                null,
                null
        );

        mockMvc.perform(put("/api/users/nonexistentuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("not found")));
    }

    @Test
    void deleteUser_Success() throws Exception {
        // Create user first
        CreateUserRequest createRequest = new CreateUserRequest(
                "deleteuser",
                "password123",
                true,
                Set.of("ROLE_USER")
        );
        userManagementService.createUser(createRequest);

        // Delete user
        mockMvc.perform(delete("/api/users/deleteuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("deleted successfully")));

        // Verify user is deleted
        mockMvc.perform(get("/api/users/deleteuser"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/users/nonexistentuser"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("not found")));
    }

    @Test
    void getUser_Success() throws Exception {
        // Create user first
        CreateUserRequest createRequest = new CreateUserRequest(
                "getuser",
                "password123",
                true,
                Set.of("ROLE_USER", "ROLE_ADMIN")
        );
        userManagementService.createUser(createRequest);

        // Get user
        mockMvc.perform(get("/api/users/getuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("getuser"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_USER")))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_ADMIN")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void getUser_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/users/nonexistentuser"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("not found")));
    }
}
