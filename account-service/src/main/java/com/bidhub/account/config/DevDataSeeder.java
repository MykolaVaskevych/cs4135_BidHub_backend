package com.bidhub.account.config;

import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.ShippingAddress;
import com.bidhub.account.model.User;
import com.bidhub.account.model.UserRole;
import com.bidhub.account.repository.AddressRepository;
import com.bidhub.account.repository.UserRepository;
import java.util.List;
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

    record Seed(String email, String password, String firstName, String lastName, UserRole role) {}

    private static final List<Seed> SEEDS = List.of(
        new Seed("admin@bidhub.local",   "Admin123!",    "Dev",   "Admin",  UserRole.ADMIN),
        new Seed("buyer1@bidhub.local",  "Buyer1Pass!",  "Bob",   "Buyer",  UserRole.BUYER),
        new Seed("buyer2@bidhub.local",  "Buyer2Pass!",  "Betty", "Buyer",  UserRole.BUYER),
        new Seed("seller1@bidhub.local", "Seller1Pass!", "Sam",   "Seller", UserRole.SELLER),
        new Seed("seller2@bidhub.local", "Seller2Pass!", "Sally", "Seller", UserRole.SELLER),
        new Seed("driver1@bidhub.local", "Driver1Pass!", "Dave",  "Driver", UserRole.DELIVERY_DRIVER)
    );

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            UserRepository userRepository,
            AddressRepository addressRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        boolean anySeeded = false;
        for (Seed s : SEEDS) {
            if (userRepository.existsByEmail(s.email())) continue;
            User u = new User();
            u.setEmail(s.email());
            u.setPasswordHash(passwordEncoder.encode(s.password()));
            u.setFirstName(s.firstName());
            u.setLastName(s.lastName());
            u.setRole(s.role());
            u.setStatus(AccountStatus.ACTIVE);
            userRepository.save(u);

            ShippingAddress addr = new ShippingAddress();
            addr.setAddressLine1("1 Main Street");
            addr.setCity("Dublin");
            addr.setCounty("Dublin");
            addr.setEircode("D01 AB12");
            addr.setIsDefault(true);
            addr.setUser(u);
            addressRepository.save(addr);

            anySeeded = true;
        }
        if (anySeeded) {
            log.info("========================================");
            log.info("Seeded dev test accounts (with default Dublin address):");
            SEEDS.forEach(s -> log.info("  [{}] {} / {}", s.role(), s.email(), s.password()));
            log.info("========================================");
        } else {
            log.info("Dev test accounts already present");
        }
    }
}
