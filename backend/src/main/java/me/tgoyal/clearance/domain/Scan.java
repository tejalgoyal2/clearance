package me.tgoyal.clearance.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import me.tgoyal.clearance.parser.Ecosystem;
import me.tgoyal.clearance.policy.PolicyStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One uploaded dependency file and its results.
 *
 * <p>Column names come from field names: Spring Boot maps {@code projectName} to
 * {@code project_name}.
 *
 * <p>Deliberately no equals/hashCode override. The id is null until the entity is saved,
 * so an id-based hashCode would change after saving and lose the object in a HashSet.
 * Object identity is correct within one persistence context, which is all this app needs.
 */
@Entity
@Table(name = "scan")
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectName;

    @Enumerated(EnumType.STRING)
    private Ecosystem ecosystem;

    private Instant createdAt;

    private int approvedCount;
    private int reviewCount;
    private int unknownCount;
    private int deniedCount;

    /**
     * Scan 1–N Component. CascadeType.PERSIST means saving the scan also saves its new
     * components, in the same transaction. Lazy by default: not loaded unless asked for.
     */
    @OneToMany(mappedBy = "scan", cascade = CascadeType.PERSIST)
    @OrderBy("name ASC")
    private List<Component> components = new ArrayList<>();

    /** For JPA only, which needs a no-argument constructor. */
    protected Scan() {
    }

    public Scan(String projectName, Ecosystem ecosystem, Instant createdAt) {
        this.projectName = projectName;
        this.ecosystem = ecosystem;
        this.createdAt = createdAt;
    }

    /** Adds a component and updates the counts. The only way to add one, so the counts stay right. */
    public void addComponent(Component component) {
        components.add(component);
        switch (component.getPolicyStatus()) {
            case APPROVED -> approvedCount++;
            case REVIEW -> reviewCount++;
            case UNKNOWN -> unknownCount++;
            case DENIED -> deniedCount++;
        }
    }

    public int countOf(PolicyStatus status) {
        return switch (status) {
            case APPROVED -> approvedCount;
            case REVIEW -> reviewCount;
            case UNKNOWN -> unknownCount;
            case DENIED -> deniedCount;
        };
    }

    public int getComponentCount() {
        return approvedCount + reviewCount + unknownCount + deniedCount;
    }

    public Long getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public Ecosystem getEcosystem() {
        return ecosystem;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Read-only view: callers must go through {@link #addComponent}. */
    public List<Component> getComponents() {
        return Collections.unmodifiableList(components);
    }
}
