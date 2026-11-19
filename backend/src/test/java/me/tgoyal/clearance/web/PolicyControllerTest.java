package me.tgoyal.clearance.web;

import me.tgoyal.clearance.policy.LicensePolicy;
import me.tgoyal.clearance.policy.PolicyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    /** A real (small) policy instead of a mock: it is a plain value object. */
    @TestConfiguration
    static class Config {
        @Bean
        LicensePolicy licensePolicy() {
            return new LicensePolicy(List.of("MIT"), List.of("MPL-2.0"), List.of("GPL-3.0-only"),
                    Map.of("Apache 2.0", "Apache-2.0"),
                    Map.of("GPL-2.0-only WITH Classpath-exception-2.0", PolicyStatus.REVIEW));
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsThePolicyAsWritten() throws Exception {
        mvc.perform(get("/api/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved[0]").value("MIT"))
                .andExpect(jsonPath("$.review[0]").value("MPL-2.0"))
                .andExpect(jsonPath("$.denied[0]").value("GPL-3.0-only"))
                .andExpect(jsonPath("$.aliases['Apache 2.0']").value("Apache-2.0"))
                .andExpect(jsonPath("$.exceptions['GPL-2.0-only WITH Classpath-exception-2.0']").value("REVIEW"));
    }
}
