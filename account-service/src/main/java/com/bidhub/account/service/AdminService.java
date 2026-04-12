package com.bidhub.account.service;

import com.bidhub.account.dto.AdminActionRequest;
import com.bidhub.account.dto.UserResponse;
import com.bidhub.account.exception.SelfActionNotAllowedException;
import com.bidhub.account.exception.UserNotFoundException;
import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.User;
import com.bidhub.account.repository.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(AccountStatus status, String search, Pageable pageable) {
        return userRepository.searchUsers(status, search, pageable).map(UserResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        return UserResponse.fromEntity(loadUser(userId));
    }

    @Transactional
    public UserResponse suspendUser(UUID adminId, UUID targetId, AdminActionRequest request) {
        requireDifferent(adminId, targetId, "suspend");
        User target = loadUser(targetId);
        target.suspend(adminId, request.reason());
        return UserResponse.fromEntity(target);
    }

    @Transactional
    public UserResponse banUser(UUID adminId, UUID targetId, AdminActionRequest request) {
        requireDifferent(adminId, targetId, "ban");
        User target = loadUser(targetId);
        target.ban(adminId, request.reason());
        return UserResponse.fromEntity(target);
    }

    @Transactional
    public UserResponse reactivateUser(UUID adminId, UUID targetId) {
        User target = loadUser(targetId);
        target.reactivate(adminId);
        return UserResponse.fromEntity(target);
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void requireDifferent(UUID adminId, UUID targetId, String action) {
        if (adminId.equals(targetId)) {
            throw new SelfActionNotAllowedException(action);
        }
    }
}
