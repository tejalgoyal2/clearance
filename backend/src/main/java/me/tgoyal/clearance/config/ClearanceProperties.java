package me.tgoyal.clearance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

/**
 * The {@code clearance.*} settings from application.yml, bound into a record.
 * Spring converts the text: "classpath:policy.yml" becomes a Resource, "5s" a Duration.
 */
@ConfigurationProperties(prefix = "clearance")
public record ClearanceProperties(Resource policyFile, DepsDev depsDev) {

    public record DepsDev(String baseUrl, Duration timeout) {
    }
}
