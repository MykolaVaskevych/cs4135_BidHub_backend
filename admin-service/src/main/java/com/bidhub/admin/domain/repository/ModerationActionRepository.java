package com.bidhub.admin.domain.repository;

import com.bidhub.admin.domain.model.ModerationAction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    List<ModerationAction> findByAdminId(UUID adminId);

    List<ModerationAction> findByTargetId(UUID targetId);
}
