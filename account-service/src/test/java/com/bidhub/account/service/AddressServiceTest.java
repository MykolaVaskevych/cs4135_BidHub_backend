package com.bidhub.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bidhub.account.dto.AddressRequest;
import com.bidhub.account.dto.AddressResponse;
import com.bidhub.account.exception.AddressNotFoundException;
import com.bidhub.account.exception.UserNotFoundException;
import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.ShippingAddress;
import com.bidhub.account.model.User;
import com.bidhub.account.model.UserRole;
import com.bidhub.account.repository.AddressRepository;
import com.bidhub.account.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @InjectMocks private AddressService addressService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("list returns empty when user has no addresses")
    void list_empty() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sampleUser()));
        List<AddressResponse> result = addressService.list(USER_ID);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("list throws when user not found")
    void list_userNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> addressService.list(USER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("create first address sets it as default")
    void create_firstAddress_isDefault() {
        User user = sampleUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(addressRepository.save(any(ShippingAddress.class))).thenAnswer(i -> {
            ShippingAddress a = i.getArgument(0);
            a.setAddressId(UUID.randomUUID());
            return a;
        });

        AddressRequest req = new AddressRequest("1 St", null, "Dublin", "Dublin", "D01", null);
        AddressResponse res = addressService.create(USER_ID, req);
        assertThat(res.isDefault()).isTrue();
    }

    @Test
    @DisplayName("create second address with isDefault=true becomes new default")
    void create_secondDefault_switchesDefault() {
        User user = sampleUser();
        ShippingAddress existing = new ShippingAddress();
        existing.setAddressId(UUID.randomUUID());
        existing.setAddressLine1("Old");
        existing.setCity("Cork");
        existing.setCounty("Cork");
        existing.setEircode("T12");
        existing.setIsDefault(true);
        user.addAddress(existing);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(addressRepository.save(any(ShippingAddress.class))).thenAnswer(i -> {
            ShippingAddress a = i.getArgument(0);
            a.setAddressId(UUID.randomUUID());
            return a;
        });

        AddressRequest req = new AddressRequest("2 St", null, "Dublin", "Dublin", "D02", true);
        AddressResponse res = addressService.create(USER_ID, req);
        assertThat(res.isDefault()).isTrue();
        assertThat(existing.getIsDefault()).isFalse();
    }

    @Test
    @DisplayName("update modifies address fields")
    void update_succeeds() {
        User user = sampleUser();
        ShippingAddress addr = new ShippingAddress();
        addr.setAddressId(UUID.randomUUID());
        addr.setAddressLine1("Old");
        addr.setCity("Cork");
        addr.setCounty("Cork");
        addr.setEircode("T12");
        addr.setIsDefault(false);
        user.addAddress(addr);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        AddressRequest req = new AddressRequest("New St", null, "Limerick", "Limerick", "V94", null);
        AddressResponse res = addressService.update(USER_ID, addr.getAddressId(), req);
        assertThat(res.addressLine1()).isEqualTo("New St");
        assertThat(res.city()).isEqualTo("Limerick");
    }

    @Test
    @DisplayName("update throws when address not owned by user")
    void update_notOwned_throws() {
        User user = sampleUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UUID fakeId = UUID.randomUUID();
        AddressRequest req = new AddressRequest("X", null, "X", "X", "X", null);
        assertThatThrownBy(() -> addressService.update(USER_ID, fakeId, req))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    @DisplayName("delete promotes next address to default when deleting default")
    void delete_default_promotesNext() {
        User user = sampleUser();
        ShippingAddress addr1 = new ShippingAddress();
        addr1.setAddressId(UUID.randomUUID());
        addr1.setAddressLine1("Addr1");
        addr1.setCity("D");
        addr1.setCounty("D");
        addr1.setEircode("D01");
        addr1.setIsDefault(true);
        user.addAddress(addr1);

        ShippingAddress addr2 = new ShippingAddress();
        addr2.setAddressId(UUID.randomUUID());
        addr2.setAddressLine1("Addr2");
        addr2.setCity("C");
        addr2.setCounty("C");
        addr2.setEircode("T12");
        addr2.setIsDefault(false);
        user.addAddress(addr2);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        addressService.delete(USER_ID, addr1.getAddressId());
        assertThat(user.getAddresses()).hasSize(1);
        assertThat(addr2.getIsDefault()).isTrue();
    }

    private User sampleUser() {
        User user = new User();
        user.setUserId(USER_ID);
        user.setEmail("addr@test.com");
        user.setPasswordHash("h");
        user.setFirstName("A");
        user.setLastName("B");
        user.setRole(UserRole.BUYER);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }
}
