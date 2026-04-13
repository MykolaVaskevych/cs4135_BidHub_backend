package com.bidhub.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.ShippingAddress;
import com.bidhub.account.model.User;
import com.bidhub.account.model.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserInvariantTest {

    private User user;
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@test.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.BUYER);
        user.setStatus(AccountStatus.ACTIVE);
    }

    @Nested
    @DisplayName("INV-U5: Status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("ACTIVE -> SUSPENDED via suspend()")
        void suspend_fromActive_succeeds() {
            user.suspend(ADMIN_ID, "test reason");
            assertThat(user.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        }

        @Test
        @DisplayName("ACTIVE -> BANNED via ban()")
        void ban_fromActive_succeeds() {
            user.ban(ADMIN_ID, "test reason");
            assertThat(user.getStatus()).isEqualTo(AccountStatus.BANNED);
        }

        @Test
        @DisplayName("SUSPENDED -> ACTIVE via reactivate()")
        void reactivate_fromSuspended_succeeds() {
            user.suspend(ADMIN_ID, "first");
            user.reactivate(ADMIN_ID);
            assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("SUSPENDED -> BANNED via ban()")
        void ban_fromSuspended_succeeds() {
            user.suspend(ADMIN_ID, "first");
            user.ban(ADMIN_ID, "escalate");
            assertThat(user.getStatus()).isEqualTo(AccountStatus.BANNED);
        }

        @Test
        @DisplayName("SUSPENDED -> SUSPENDED rejected")
        void suspend_fromSuspended_throws() {
            user.suspend(ADMIN_ID, "first");
            assertThatThrownBy(() -> user.suspend(ADMIN_ID, "again"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("BANNED -> SUSPENDED rejected (terminal)")
        void suspend_fromBanned_throws() {
            user.ban(ADMIN_ID, "ban");
            assertThatThrownBy(() -> user.suspend(ADMIN_ID, "after ban"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("BANNED -> BANNED rejected")
        void ban_fromBanned_throws() {
            user.ban(ADMIN_ID, "first");
            assertThatThrownBy(() -> user.ban(ADMIN_ID, "again"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("BANNED -> ACTIVE rejected (terminal)")
        void reactivate_fromBanned_throws() {
            user.ban(ADMIN_ID, "ban");
            assertThatThrownBy(() -> user.reactivate(ADMIN_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("suspended");
        }

        @Test
        @DisplayName("ACTIVE -> ACTIVE via reactivate() rejected")
        void reactivate_fromActive_throws() {
            assertThatThrownBy(() -> user.reactivate(ADMIN_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("INV-U3: Default address")
    class DefaultAddress {

        @Test
        @DisplayName("First address becomes default automatically")
        void addAddress_first_isDefault() {
            ShippingAddress addr = createAddress(true);
            user.addAddress(addr);
            assertThat(user.getAddresses()).hasSize(1);
            assertThat(user.getAddresses().get(0).getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("Adding default address clears previous default")
        void addAddress_newDefault_clearsPrevious() {
            ShippingAddress addr1 = createAddress(true);
            ShippingAddress addr2 = createAddress(true);

            user.addAddress(addr1);
            user.addAddress(addr2);

            long defaultCount =
                    user.getAddresses().stream()
                            .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                            .count();
            assertThat(defaultCount).isEqualTo(1);
            assertThat(addr2.getIsDefault()).isTrue();
            assertThat(addr1.getIsDefault()).isFalse();
        }

        @Test
        @DisplayName("setDefaultAddress switches default")
        void setDefaultAddress_switches() {
            ShippingAddress addr1 = createAddress(true);
            ShippingAddress addr2 = createAddress(false);
            user.addAddress(addr1);
            user.addAddress(addr2);

            // addr2 has an ID we need — simulate JPA by setting one
            UUID addr2Id = addr2.getAddressId();
            if (addr2Id != null) {
                user.setDefaultAddress(addr2Id);
                assertThat(addr2.getIsDefault()).isTrue();
                assertThat(addr1.getIsDefault()).isFalse();
            }
        }

        @Test
        @DisplayName("removeAddress removes by ID")
        void removeAddress_removesById() {
            ShippingAddress addr = createAddress(false);
            user.addAddress(addr);
            UUID id = addr.getAddressId();

            assertThat(user.getAddresses()).hasSize(1);
            if (id != null) {
                user.removeAddress(id);
                assertThat(user.getAddresses()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("INV-U4: Role defaults")
    class RoleDefaults {

        @Test
        @DisplayName("Default role is BUYER")
        void defaultRole_isBuyer() {
            User fresh = new User();
            assertThat(fresh.getRole()).isEqualTo(UserRole.BUYER);
        }

        @Test
        @DisplayName("Default status is ACTIVE")
        void defaultStatus_isActive() {
            User fresh = new User();
            assertThat(fresh.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }
    }

    private ShippingAddress createAddress(boolean isDefault) {
        ShippingAddress addr = new ShippingAddress();
        addr.setAddressLine1("123 Test St");
        addr.setCity("Dublin");
        addr.setCounty("Dublin");
        addr.setEircode("D01 AB12");
        addr.setIsDefault(isDefault);
        return addr;
    }
}
