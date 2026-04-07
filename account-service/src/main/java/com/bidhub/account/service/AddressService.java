package com.bidhub.account.service;

import com.bidhub.account.dto.AddressRequest;
import com.bidhub.account.dto.AddressResponse;
import com.bidhub.account.exception.AddressNotFoundException;
import com.bidhub.account.exception.UserNotFoundException;
import com.bidhub.account.model.ShippingAddress;
import com.bidhub.account.model.User;
import com.bidhub.account.repository.AddressRepository;
import com.bidhub.account.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public AddressService(UserRepository userRepository, AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        User user = loadUser(userId);
        return user.getAddresses().stream().map(AddressResponse::fromEntity).toList();
    }

    @Transactional
    public AddressResponse create(UUID userId, AddressRequest request) {
        User user = loadUser(userId);

        ShippingAddress address = new ShippingAddress();
        applyRequest(address, request);

        boolean firstAddress = user.getAddresses().isEmpty();
        boolean requestedDefault = Boolean.TRUE.equals(request.isDefault());
        address.setIsDefault(firstAddress || requestedDefault);

        user.addAddress(address);
        ShippingAddress saved = addressRepository.save(address);
        return AddressResponse.fromEntity(saved);
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, AddressRequest request) {
        User user = loadUser(userId);
        ShippingAddress address = findOwnedAddress(user, addressId);
        applyRequest(address, request);
        return AddressResponse.fromEntity(address);
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        User user = loadUser(userId);
        ShippingAddress address = findOwnedAddress(user, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        user.removeAddress(addressId);

        if (wasDefault && !user.getAddresses().isEmpty()) {
            user.getAddresses().get(0).setIsDefault(true);
        }
    }

    @Transactional
    public AddressResponse setDefault(UUID userId, UUID addressId) {
        User user = loadUser(userId);
        findOwnedAddress(user, addressId);
        user.setDefaultAddress(addressId);
        return user.getAddresses().stream()
                .filter(a -> a.getAddressId().equals(addressId))
                .findFirst()
                .map(AddressResponse::fromEntity)
                .orElseThrow(() -> new AddressNotFoundException(userId, addressId));
    }

    private void applyRequest(ShippingAddress address, AddressRequest request) {
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setCounty(request.county());
        address.setEircode(request.eircode());
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private ShippingAddress findOwnedAddress(User user, UUID addressId) {
        return user.getAddresses().stream()
                .filter(a -> a.getAddressId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new AddressNotFoundException(user.getUserId(), addressId));
    }
}
