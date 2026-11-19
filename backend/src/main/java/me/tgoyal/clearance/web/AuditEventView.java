package me.tgoyal.clearance.web;

import me.tgoyal.clearance.domain.AuditAction;
import me.tgoyal.clearance.domain.AuditEvent;

import java.time.Instant;

public record AuditEventView(Long id,
                             Long componentId,
                             String actor,
                             AuditAction action,
                             String detail,
                             Instant timestamp) {

    public static AuditEventView from(AuditEvent event) {
        // Reading the id of a lazy proxy does not load it from the database.
        Long componentId = event.getComponent() == null ? null : event.getComponent().getId();
        return new AuditEventView(event.getId(), componentId, event.getActor(), event.getAction(),
                event.getDetail(), event.getTimestamp());
    }
}
