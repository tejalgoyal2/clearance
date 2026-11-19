package me.tgoyal.clearance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * One entry in a scan's audit trail. Append-only, enforced three times:
 * <ol>
 *   <li>no setters: once built, the object can't be changed;</li>
 *   <li>{@code @Immutable}: Hibernate never issues an UPDATE for it;</li>
 *   <li>a database trigger rejects UPDATE and DELETE (see V1__create_tables.sql).</li>
 * </ol>
 */
@Entity
@Immutable
@Table(name = "audit_event")
public class AuditEvent {

    /** No login in this demo, so uploads and exports have no named actor. */
    public static final String ANONYMOUS = "anonymous";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id")
    private Scan scan;

    /** Null for events about the whole scan. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id")
    private Component component;

    private String actor;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    private String detail;

    @Column(name = "occurred_at")
    private Instant timestamp;

    protected AuditEvent() {
    }

    public AuditEvent(Scan scan, Component component, String actor, AuditAction action, String detail, Instant timestamp) {
        this.scan = scan;
        this.component = component;
        this.actor = actor;
        this.action = action;
        this.detail = detail;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public Scan getScan() {
        return scan;
    }

    public Component getComponent() {
        return component;
    }

    public String getActor() {
        return actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
