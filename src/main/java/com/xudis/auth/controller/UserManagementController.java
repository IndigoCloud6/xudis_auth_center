package com.xudis.auth.controller;

import com.xudis.auth.dto.CreateUserRequest;
import com.xudis.auth.dto.UpdateUserRequest;
import com.xudis.auth.dto.UserResponse;
import com.xudis.auth.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserManagementController {

    private final UserManagementService userManagementService;

    @PostMapping
    public ResponseEntity<Object> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserResponse response = userManagementService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserManagementService.UserAlreadyExistsException e) {
            log.warn("User creation failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        } catch (Exception e) {
            log.error("Error creating user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{username}")
    public ResponseEntity<Object> updateUser(
            @PathVariable String username,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            UserResponse response = userManagementService.updateUser(username, request);
            return ResponseEntity.ok(response);
        } catch (UserManagementService.UserNotFoundException e) {
            log.warn("User update failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error updating user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Object> deleteUser(@PathVariable String username) {
        try {
            userManagementService.deleteUser(username);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (UserManagementService.UserNotFoundException e) {
            log.warn("User deletion failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error deleting user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<Object> getUser(@PathVariable String username) {
        try {
            UserResponse response = userManagementService.getUser(username);
            return ResponseEntity.ok(response);
        } catch (UserManagementService.UserNotFoundException e) {
            log.warn("User retrieval failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            log.error("Error getting user", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
