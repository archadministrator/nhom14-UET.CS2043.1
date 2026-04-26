package com.example.auction.service;

import com.example.auction.model.AuditLog;
import com.example.auction.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Ghi lại nhật ký các hành động quan trọng (Bất đồng bộ để không ảnh hưởng đến hiệu năng).
     */
    @Async
    public void logAction(String entityName, Long entityId, AuditLog.ActionType action, String performedBy, String details) {
        AuditLog auditLog = new AuditLog(entityName, entityId, action, performedBy, details);
        auditLogRepository.save(auditLog);
    }
}
