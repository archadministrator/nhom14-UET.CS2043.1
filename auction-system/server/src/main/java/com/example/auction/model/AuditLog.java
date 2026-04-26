package com.example.auction.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    public enum ActionType {
        CREATE, UPDATE, DELETE, SOFT_DELETE, STATE_CHANGE, BID
    }

    private String entityName;
    private Long entityId;
    
    @Enumerated(EnumType.STRING)
    private ActionType action;
    
    private String performedBy;
    private String details;

    public AuditLog() {
    }

    public AuditLog(String entityName, Long entityId, ActionType action, String performedBy, String details) {
        this.entityName = entityName;
        this.entityId = entityId;
        this.action = action;
        this.performedBy = performedBy;
        this.details = details;
    }

    // Getters and Setters
    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
