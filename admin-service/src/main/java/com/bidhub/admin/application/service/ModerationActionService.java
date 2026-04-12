package com.bidhub.admin.application.service;

import com.bidhub.admin.application.dto.ModerationActionResponse;
import com.bidhub.admin.application.dto.PerformModerationRequest;
import com.bidhub.admin.domain.model.ModerationAction;
import com.bidhub.admin.domain.repository.ModerationActionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ModerationActionService {

    private final ModerationActionRepository moderationActionRepository;

    public ModerationActionService(ModerationActionRepository moderationActionRepository) {
        this.moderationActionRepository = moderationActionRepository;
    }

    public ModerationActionResponse performAction(UUID adminId, PerformModerationRequest req) {
        ModerationAction action =
                ModerationAction.create(
                        adminId,
                        req.targetId(),
                        req.targetType(),
                        req.actionType(),
                        req.reason());
        return ModerationActionResponse.from(moderationActionRepository.save(action));
    }

    @Transactional(readOnly = true)
    public List<ModerationActionResponse> listActions(UUID adminId, UUID targetId) {
        if (adminId != null) {
            return moderationActionRepository.findByAdminId(adminId).stream()
                    .map(ModerationActionResponse::from)
                    .toList();
        }
        if (targetId != null) {
            return moderationActionRepository.findByTargetId(targetId).stream()
                    .map(ModerationActionResponse::from)
                    .toList();
        }
        return moderationActionRepository.findAll().stream()
                .map(ModerationActionResponse::from)
                .toList();
    }
}
