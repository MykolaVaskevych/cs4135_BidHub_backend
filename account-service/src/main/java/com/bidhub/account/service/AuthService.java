package com.bidhub.account.service;

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
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(resolveRole(request.role()));
        user.setStatus(AccountStatus.ACTIVE);

        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(user.getStatus());
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(UUID userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(user.getStatus());
        }

        return buildAuthResponse(user);
    }

    private UserRole resolveRole(UserRole requested) {
        if (requested == null || requested == UserRole.ADMIN) {
            return UserRole.BUYER;
        }
        return requested;
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(
                token,
                jwtService.getExpirationMs(),
                user.getUserId(),
                user.getEmail(),
                user.getRole());
    }
}
