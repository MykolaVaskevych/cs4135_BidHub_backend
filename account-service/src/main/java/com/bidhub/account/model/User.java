package com.bidhub.account.model;

import jakarta.persistence.*;
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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShippingAddress> addresses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void suspend(UUID adminId, String reason) {
        if (this.status == AccountStatus.BANNED) {
            throw new IllegalStateException("Cannot suspend a banned user");
        }
        this.status = AccountStatus.SUSPENDED;
    }

    public void ban(UUID adminId, String reason) {
        if (this.status == AccountStatus.BANNED) {
            throw new IllegalStateException("User is already banned");
        }
        this.status = AccountStatus.BANNED;
    }

    public void reactivate(UUID adminId) {
        if (this.status != AccountStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended users can be reactivated");
        }
        this.status = AccountStatus.ACTIVE;
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
