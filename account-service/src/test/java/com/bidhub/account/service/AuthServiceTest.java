package com.bidhub.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bidhub.account.dto.AuthResponse;
import com.bidhub.account.dto.LoginRequest;
import com.bidhub.account.dto.RegisterRequest;
import com.bidhub.account.exception.AccountNotActiveException;
import com.bidhub.account.exception.EmailAlreadyExistsException;
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
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("register creates user and returns token")
    void register_happyPath() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Pass123!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        RegisterRequest req = new RegisterRequest("new@test.com", "Pass123!", "First", "Last", UserRole.BUYER);
        AuthResponse res = authService.register(req);

        assertThat(res.email()).isEqualTo("new@test.com");
        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.role()).isEqualTo(UserRole.BUYER);
    }

    @Test
    @DisplayName("register with duplicate email throws EmailAlreadyExistsException")
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);
        RegisterRequest req = new RegisterRequest("dup@test.com", "Pass123!", "F", "L", UserRole.BUYER);
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    @DisplayName("register with ADMIN role defaults to BUYER")
    void register_adminRole_defaultsToBuyer() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateToken(any())).thenReturn("jwt");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        RegisterRequest req = new RegisterRequest("x@test.com", "Pass123!", "F", "L", UserRole.ADMIN);
        AuthResponse res = authService.register(req);
        assertThat(res.role()).isEqualTo(UserRole.BUYER);
    }

    @Test
    @DisplayName("login with valid credentials returns token")
    void login_happyPath() {
        User user = createUser(AccountStatus.ACTIVE);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass123!", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse res = authService.login(new LoginRequest("test@test.com", "Pass123!"));
        assertThat(res.token()).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("login with wrong password throws InvalidCredentialsException")
    void login_wrongPassword_throws() {
        User user = createUser(AccountStatus.ACTIVE);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@test.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login with nonexistent email throws InvalidCredentialsException")
    void login_unknownEmail_throws() {
        when(userRepository.findByEmail("no@test.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest("no@test.com", "Pass123!")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login with SUSPENDED account throws AccountNotActiveException")
    void login_suspended_throws() {
        User user = createUser(AccountStatus.SUSPENDED);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass123!", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@test.com", "Pass123!")))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    @DisplayName("login with BANNED account throws AccountNotActiveException")
    void login_banned_throws() {
        User user = createUser(AccountStatus.BANNED);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass123!", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@test.com", "Pass123!")))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    @DisplayName("refreshToken returns new token for active user")
    void refreshToken_active_succeeds() {
        User user = createUser(AccountStatus.ACTIVE);
        UUID userId = user.getUserId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("new-jwt");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse res = authService.refreshToken(userId);
        assertThat(res.token()).isEqualTo("new-jwt");
    }

    @Test
    @DisplayName("refreshToken for suspended user throws AccountNotActiveException")
    void refreshToken_suspended_throws() {
        User user = createUser(AccountStatus.SUSPENDED);
        UUID userId = user.getUserId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refreshToken(userId))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    @DisplayName("refreshToken for nonexistent user throws UserNotFoundException")
    void refreshToken_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.refreshToken(id))
                .isInstanceOf(UserNotFoundException.class);
    }

    private User createUser(AccountStatus status) {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.BUYER);
        user.setStatus(status);
        return user;
    }
}
