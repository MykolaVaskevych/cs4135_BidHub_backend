package com.bidhub.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bidhub.account.dto.ChangePasswordRequest;
import com.bidhub.account.dto.UpdateProfileRequest;
import com.bidhub.account.dto.UserResponse;
import com.bidhub.account.exception.InvalidCredentialsException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("getById returns user response")
    void getById_found() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser()));
        UserResponse res = userService.getById(USER_ID);
        assertThat(res.email()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("getById throws when user not found")
    void getById_notFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getById(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("updateProfile updates first and last name")
    void updateProfile_succeeds() {
        User user = sampleUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserResponse res = userService.updateProfile(USER_ID, new UpdateProfileRequest("New", "Name"));
        assertThat(res.firstName()).isEqualTo("New");
        assertThat(res.lastName()).isEqualTo("Name");
    }

    @Test
    @DisplayName("changePassword with correct current password succeeds")
    void changePassword_correct_succeeds() {
        User user = sampleUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass1!", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("new-hashed");

        userService.changePassword(USER_ID, new ChangePasswordRequest("OldPass1!", "NewPass1!"));
        assertThat(user.getPasswordHash()).isEqualTo("new-hashed");
    }

    @Test
    @DisplayName("changePassword with wrong current password throws")
    void changePassword_wrongCurrent_throws() {
        User user = sampleUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(USER_ID,
                new ChangePasswordRequest("wrong", "NewPass1!")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private User sampleUser() {
        User user = new User();
        user.setUserId(USER_ID);
        user.setEmail("user@test.com");
        user.setPasswordHash("hashed");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setRole(UserRole.BUYER);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }
}
