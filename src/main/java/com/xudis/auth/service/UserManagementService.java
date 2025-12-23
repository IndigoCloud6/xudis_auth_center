package com.xudis.auth.service;

import com.xudis.auth.dto.CreateUserRequest;
import com.xudis.auth.dto.UpdateUserRequest;
import com.xudis.auth.dto.UserResponse;
import com.xudis.auth.entity.Authority;
import com.xudis.auth.entity.User;
import com.xudis.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        // Set authorities (default to ROLE_USER if not provided)
        Set<String> authoritiesSet = request.getAuthorities();
        if (authoritiesSet == null || authoritiesSet.isEmpty()) {
            authoritiesSet = Set.of("ROLE_USER");
        }

        Set<Authority> authorities = new HashSet<>();
        for (String authorityName : authoritiesSet) {
            Authority authority = new Authority(user, authorityName);
            authorities.add(authority);
        }
        user.setAuthorities(authorities);

        // Save user
        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", savedUser.getUsername());

        return toUserResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(String username, UpdateUserRequest request) {
        log.info("Updating user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            log.info("Password updated for user: {}", username);
        }

        // Update enabled status if provided
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        // Update authorities if provided (full replacement)
        if (request.getAuthorities() != null) {
            // Remove all existing authorities
            user.getAuthorities().clear();
            
            // Flush changes to database to trigger orphan removal
            userRepository.saveAndFlush(user);

            // Add new authorities
            for (String authorityName : request.getAuthorities()) {
                Authority authority = new Authority(user, authorityName);
                user.getAuthorities().add(authority);
            }
            log.info("Authorities updated for user: {}", username);
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getUsername());

        return toUserResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(String username) {
        log.info("Deleting user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        // The authorities will be automatically deleted due to CascadeType.ALL and orphanRemoval = true
        userRepository.delete(user);
        log.info("User deleted successfully: {}", username);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(String username) {
        log.info("Getting user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        Set<String> authorities = user.getAuthorities().stream()
                .map(Authority::getAuthority)
                .collect(Collectors.toSet());

        return new UserResponse(user.getUsername(), user.getEnabled(), authorities);
    }

    // Custom exceptions
    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
