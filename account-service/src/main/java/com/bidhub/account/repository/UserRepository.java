package com.bidhub.account.repository;

import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByStatus(AccountStatus status, Pageable pageable);

    @Query(
            "SELECT u FROM User u WHERE "
                    + "(:status IS NULL OR u.status = :status) AND "
                    + "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
                    + "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
                    + "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<User> searchUsers(
            @Param("status") AccountStatus status,
            @Param("search") String search,
            Pageable pageable);
}
