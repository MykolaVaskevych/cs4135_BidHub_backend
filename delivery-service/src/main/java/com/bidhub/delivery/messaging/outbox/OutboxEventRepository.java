package com.bidhub.delivery.messaging.outbox;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OutboxEvent> findForUpdateByStatus(@Param("status") OutboxStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT o FROM OutboxEvent o "
                    + "WHERE o.status = :status AND o.processingStartedAt < :before "
                    + "ORDER BY o.processingStartedAt ASC")
    List<OutboxEvent> findForUpdateStaleByStatus(
            @Param("status") OutboxStatus status,
            @Param("before") Instant before,
            Pageable pageable);
}
