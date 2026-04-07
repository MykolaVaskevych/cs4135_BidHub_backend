package com.bidhub.account.config;

import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.User;
import com.bidhub.account.model.UserRole;
import com.bidhub.account.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final String ADMIN_EMAIL = "admin@bidhub.local";
    private static final String ADMIN_PASSWORD = "Admin123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Dev admin already present: {}", ADMIN_EMAIL);
            return;
        }

        User admin = new User();
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setFirstName("Dev");
        admin.setLastName("Admin");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        userRepository.save(admin);

        log.info("========================================");
        log.info("Seeded dev admin account");
        log.info("  email:    {}", ADMIN_EMAIL);
        log.info("  password: {}", ADMIN_PASSWORD);
        log.info("========================================");
    }
}
