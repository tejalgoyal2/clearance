package me.tgoyal.clearance.web;

import jakarta.validation.Valid;
import me.tgoyal.clearance.service.DecisionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/components")
public class ComponentController {

    private final DecisionService decisionService;

    public ComponentController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    /**
     * Accept or reject a component. @Valid runs the checks on {@link DecisionRequest}
     * first; an invalid body never reaches the service.
     */
    @PostMapping("/{id}/decision")
    public ComponentView decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ComponentView.from(decisionService.decide(id, request.decision(), request.reviewer().trim(),
                request.comment().trim()));
    }
}
