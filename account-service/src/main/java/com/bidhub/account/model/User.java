package com.bidhub.account.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.BUYER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column private LocalDateTime suspendedAt;
    @Column(length = 500) private String suspensionReason;
    @Column private UUID suspendedBy;

    @Column private LocalDateTime bannedAt;
    @Column(length = 500) private String banReason;
    @Column private UUID bannedBy;

    /**
     * Any JWT with {@code iat} strictly before this instant must be rejected. Set to now() on
     * suspend/ban so revocation is immediate; intentionally not cleared on reactivate — the user
     * must log in again to get a fresh token.
     */
    @Column private Instant tokensInvalidAfter;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShippingAddress> addresses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void suspend(UUID adminId, String reason) {
        if (this.status != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Can only suspend an ACTIVE user, current status: " + this.status);
        }
        this.status = AccountStatus.SUSPENDED;
        this.suspendedAt = LocalDateTime.now();
        this.suspensionReason = reason;
        this.suspendedBy = adminId;
        this.tokensInvalidAfter = Instant.now();
    }

    public void ban(UUID adminId, String reason) {
        if (this.status != AccountStatus.ACTIVE && this.status != AccountStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Can only ban an ACTIVE or SUSPENDED user, current status: " + this.status);
        }
        this.status = AccountStatus.BANNED;
        this.bannedAt = LocalDateTime.now();
        this.banReason = reason;
        this.bannedBy = adminId;
        this.tokensInvalidAfter = Instant.now();
    }

    public void reactivate(UUID adminId) {
        if (this.status != AccountStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended users can be reactivated");
        }
        this.status = AccountStatus.ACTIVE;
        // tokensInvalidAfter intentionally NOT cleared — the suspended user's old JWT must stay
        // dead even after reactivation; they have to log in again to get a fresh one.
    }

    public void addAddress(ShippingAddress address) {
        address.setUser(this);
        if (address.getIsDefault()) {
            clearDefaultAddress();
        }
        this.addresses.add(address);
    }

    public void removeAddress(UUID addressId) {
        this.addresses.removeIf(a -> a.getAddressId().equals(addressId));
    }

    public void setDefaultAddress(UUID addressId) {
        clearDefaultAddress();
        this.addresses.stream()
                .filter(a -> a.getAddressId().equals(addressId))
                .findFirst()
                .ifPresent(a -> a.setIsDefault(true));
    }

    private void clearDefaultAddress() {
        this.addresses.forEach(a -> a.setIsDefault(false));
    }
}
