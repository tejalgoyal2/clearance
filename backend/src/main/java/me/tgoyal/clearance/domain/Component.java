package me.tgoyal.clearance.domain;

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
import jakarta.persistence.Version;
import me.tgoyal.clearance.policy.PolicyStatus;

import java.time.Instant;

/**
 * One dependency in a scan: its licence, the policy's verdict, and the reviewer's decision.
 *
 * <p>The policy verdict is fixed when the scan runs. A reviewer's decision is recorded
 * next to it and never changes it: a DENIED component that was accepted stays DENIED,
 * with the reviewer's name and reason for the exception.
 *
 * <p>Named after the spec. Note it is unrelated to Spring's {@code @Component} annotation,
 * which this project never uses.
 */
@Entity
@Table(name = "component")
public class Component {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Component N–1 Scan. Lazy: the scan is only loaded if its fields are read. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id")
    private Scan scan;

    private String name;
    private String version;

    /** The licence as found, before normalization. Null if none was found. */
    private String rawLicense;

    /** Where the licence came from: "lockfile" or "deps.dev". */
    private String licenseSource;

    /** The normalized SPDX expression. Null if it could not be parsed. */
    private String spdxExpression;

    @Enumerated(EnumType.STRING)
    private PolicyStatus policyStatus;

    /** Why the status is UNKNOWN: no licence found, or the parse error. */
    private String policyNote;

    @Enumerated(EnumType.STRING)
    private Decision decision = Decision.NONE;

    private String decidedBy;
    private Instant decidedAt;
    private String decisionComment;

    /** Optimistic lock. If two reviewers decide at once, the second commit fails instead of overwriting. */
    @Version
    private long rowVersion;

    protected Component() {
    }

    public Component(Scan scan, String name, String version, String rawLicense, String licenseSource,
                     String spdxExpression, PolicyStatus policyStatus, String policyNote) {
        this.scan = scan;
        this.name = name;
        this.version = version;
        this.rawLicense = rawLicense;
        this.licenseSource = licenseSource;
        this.spdxExpression = spdxExpression;
        this.policyStatus = policyStatus;
        this.policyNote = policyNote;
    }

    /**
     * Records a reviewer's decision. Allowed once.
     *
     * @throws AlreadyDecidedException if the component already has a decision
     */
    public void decide(Decision decision, String reviewer, String comment, Instant now) {
        if (decision == Decision.NONE) {
            throw new IllegalArgumentException("A decision must be ACCEPTED or REJECTED");
        }
        if (this.decision != Decision.NONE) {
            throw new AlreadyDecidedException("Component " + id + " was already " + this.decision
                    + " by " + decidedBy + " at " + decidedAt);
        }
        this.decision = decision;
        this.decidedBy = reviewer;
        this.decisionComment = comment;
        this.decidedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Scan getScan() {
        return scan;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getRawLicense() {
        return rawLicense;
    }

    public String getLicenseSource() {
        return licenseSource;
    }

    public String getSpdxExpression() {
        return spdxExpression;
    }

    public PolicyStatus getPolicyStatus() {
        return policyStatus;
    }

    public String getPolicyNote() {
        return policyNote;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionComment() {
        return decisionComment;
    }
}
