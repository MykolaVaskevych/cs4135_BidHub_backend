package com.bidhub.account.repository;

import com.bidhub.account.model.ShippingAddress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<ShippingAddress, UUID> {}
