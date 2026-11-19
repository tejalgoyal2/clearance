package me.tgoyal.clearance.service;

import me.tgoyal.clearance.domain.AuditAction;
import me.tgoyal.clearance.domain.AuditEvent;
import me.tgoyal.clearance.domain.Component;
import me.tgoyal.clearance.domain.Decision;
import me.tgoyal.clearance.repository.AuditEventRepository;
import me.tgoyal.clearance.repository.ComponentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class DecisionService {

    private final ComponentRepository componentRepository;
    private final AuditEventRepository auditEventRepository;
    private final Clock clock;

    public DecisionService(ComponentRepository componentRepository,
                           AuditEventRepository auditEventRepository,
                           Clock clock) {
        this.componentRepository = componentRepository;
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
    }

    /**
     * Records a reviewer's decision and its audit event in one transaction: both are
     * saved, or (if anything throws) neither is.
     *
     * <p>There is no explicit save of the component. It was loaded in this transaction,
     * so Hibernate notices the change ("dirty checking") and writes it at commit.
     *
     * @throws NotFoundException if there is no such component
     * @throws me.tgoyal.clearance.domain.AlreadyDecidedException if it was already decided
     */
    @Transactional
    public Component decide(Long componentId, Decision decision, String reviewer, String comment) {
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new NotFoundException("Component " + componentId + " not found"));

        Instant now = clock.instant();
        component.decide(decision, reviewer, comment, now);

        String detail = decision + " " + component.getName() + "@" + component.getVersion()
                + " (" + component.getPolicyStatus() + "): " + comment;
        auditEventRepository.save(new AuditEvent(component.getScan(), component, reviewer,
                AuditAction.DECISION_MADE, detail, now));
        return component;
    }
}
