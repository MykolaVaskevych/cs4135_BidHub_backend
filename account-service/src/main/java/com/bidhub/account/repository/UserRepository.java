package com.bidhub.account.repository;

import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByStatus(AccountStatus status);
}
