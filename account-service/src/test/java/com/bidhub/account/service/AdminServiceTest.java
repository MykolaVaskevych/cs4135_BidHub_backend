package com.bidhub.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bidhub.account.dto.AdminActionRequest;
import com.bidhub.account.dto.UserSummaryResponse;
import com.bidhub.account.exception.SelfActionNotAllowedException;
import com.bidhub.account.exception.UserNotFoundException;
import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.User;
import com.bidhub.account.model.UserRole;
import com.bidhub.account.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private AdminService adminService;

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();

    @Test
    @DisplayName("listUsers returns paginated results")
    void listUsers_returnsPaginated() {
        Page<User> page = new PageImpl<>(List.of(sampleUser(TARGET_ID)));
        when(userRepository.searchUsers(any(), any(), any())).thenReturn(page);

        Page<UserSummaryResponse> result =
                adminService.listUsers(null, null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).email()).isEqualTo("target@test.com");
    }

    @Test
    @DisplayName("listUsers filters by status")
    void listUsers_filtersByStatus() {
        Page<User> page = new PageImpl<>(List.of(sampleUser(TARGET_ID)));
        when(userRepository.searchUsers(any(), any(), any())).thenReturn(page);

        Page<UserSummaryResponse> result =
                adminService.listUsers(AccountStatus.ACTIVE, null, PageRequest.of(0, 10));
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("getUser returns summary without addresses")
    void getUser_returnsSummary() {
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(sampleUser(TARGET_ID)));
        UserSummaryResponse res = adminService.getUser(TARGET_ID);
        assertThat(res.email()).isEqualTo("target@test.com");
    }

    @Test
    @DisplayName("getUser throws when not found")
    void getUser_notFound_throws() {
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adminService.getUser(TARGET_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("suspendUser changes status to SUSPENDED")
    void suspendUser_succeeds() {
        User target = sampleUser(TARGET_ID);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        adminService.suspendUser(ADMIN_ID, TARGET_ID, new AdminActionRequest("reason"));
        assertThat(target.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    @DisplayName("suspendUser on self throws SelfActionNotAllowedException")
    void suspendUser_self_throws() {
        assertThatThrownBy(() -> adminService.suspendUser(ADMIN_ID, ADMIN_ID,
                new AdminActionRequest("reason")))
                .isInstanceOf(SelfActionNotAllowedException.class);
    }

    @Test
    @DisplayName("banUser changes status to BANNED")
    void banUser_succeeds() {
        User target = sampleUser(TARGET_ID);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        adminService.banUser(ADMIN_ID, TARGET_ID, new AdminActionRequest("reason"));
        assertThat(target.getStatus()).isEqualTo(AccountStatus.BANNED);
    }

    @Test
    @DisplayName("banUser on self throws SelfActionNotAllowedException")
    void banUser_self_throws() {
        assertThatThrownBy(() -> adminService.banUser(ADMIN_ID, ADMIN_ID,
                new AdminActionRequest("reason")))
                .isInstanceOf(SelfActionNotAllowedException.class);
    }

    @Test
    @DisplayName("reactivateUser changes SUSPENDED to ACTIVE")
    void reactivateUser_succeeds() {
        User target = sampleUser(TARGET_ID);
        target.suspend(ADMIN_ID, "first");
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        adminService.reactivateUser(ADMIN_ID, TARGET_ID);
        assertThat(target.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("reactivateUser on self throws SelfActionNotAllowedException")
    void reactivateUser_self_throws() {
        assertThatThrownBy(() -> adminService.reactivateUser(ADMIN_ID, ADMIN_ID))
                .isInstanceOf(SelfActionNotAllowedException.class);
    }

    private User sampleUser(UUID userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("target@test.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Target");
        user.setLastName("User");
        user.setRole(UserRole.BUYER);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }
}
