package me.tgoyal.clearance.web;

import me.tgoyal.clearance.TestData;
import me.tgoyal.clearance.domain.AlreadyDecidedException;
import me.tgoyal.clearance.domain.Component;
import me.tgoyal.clearance.domain.Decision;
import me.tgoyal.clearance.policy.PolicyStatus;
import me.tgoyal.clearance.service.DecisionService;
import me.tgoyal.clearance.service.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComponentController.class)
class ComponentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DecisionService decisionService;

    private static final String ACCEPT_WITH_COMMENT = """
            { "decision": "ACCEPTED", "reviewer": "tejal", "comment": "Internal tool, never distributed" }
            """;

    @Test
    void decisionReturns200WithTheUpdatedComponent() throws Exception {
        Component component = TestData.component(TestData.scan(1), 5, "gpl-thing", PolicyStatus.DENIED);
        component.decide(Decision.ACCEPTED, "tejal", "Internal tool, never distributed", TestData.NOW);
        when(decisionService.decide(5L, Decision.ACCEPTED, "tejal", "Internal tool, never distributed"))
                .thenReturn(component);

        mvc.perform(post("/api/components/5/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT_WITH_COMMENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("ACCEPTED"))
                .andExpect(jsonPath("$.decidedBy").value("tejal"))
                // The decision never changes the policy verdict.
                .andExpect(jsonPath("$.policyStatus").value("DENIED"));
    }

    @Test
    void missingCommentReturns400() throws Exception {
        mvc.perform(post("/api/components/5/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decision": "ACCEPTED", "reviewer": "tejal" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("comment")));

        verifyNoInteractions(decisionService);
    }

    @Test
    void decisionNoneReturns400() throws Exception {
        mvc.perform(post("/api/components/5/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "decision": "NONE", "reviewer": "tejal", "comment": "hmm" }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(decisionService);
    }

    @Test
    void unreadableJsonReturns400() throws Exception {
        mvc.perform(post("/api/components/5/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownComponentReturns404() throws Exception {
        when(decisionService.decide(eq(404L), any(), any(), any()))
                .thenThrow(new NotFoundException("Component 404 not found"));

        mvc.perform(post("/api/components/404/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT_WITH_COMMENT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Component 404 not found"));
    }

    @Test
    void secondDecisionReturns409() throws Exception {
        when(decisionService.decide(eq(5L), any(), any(), any()))
                .thenThrow(new AlreadyDecidedException("Component 5 was already ACCEPTED"));

        mvc.perform(post("/api/components/5/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT_WITH_COMMENT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
