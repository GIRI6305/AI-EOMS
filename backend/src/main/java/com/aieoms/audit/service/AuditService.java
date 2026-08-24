package com.aieoms.audit.service;

import com.aieoms.audit.entity.AuditLog;
import com.aieoms.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditLog record(
            Long userId,
            String action,
            String entityType,
            String entityId,
            String description,
            String ipAddress,
            String userAgent
    ) {
        AuditLog log = new AuditLog();

        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);

        return repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findByUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
