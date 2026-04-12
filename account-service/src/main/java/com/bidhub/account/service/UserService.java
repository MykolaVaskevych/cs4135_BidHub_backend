package com.bidhub.account.service;

import com.bidhub.account.dto.ChangePasswordRequest;
import com.bidhub.account.dto.UpdateProfileRequest;
import com.bidhub.account.dto.UserResponse;
import com.bidhub.account.exception.InvalidCredentialsException;
import com.bidhub.account.exception.UserNotFoundException;
import com.bidhub.account.model.User;
import com.bidhub.account.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId) {
        User user = loadUser(userId);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = loadUser(userId);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = loadUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }
}
