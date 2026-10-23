package com.example.protec.health;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final HealthDatabaseProvider healthDatabaseProvider;
    private final Environment environment;
    private final BuildProperties buildProperties;

    public HealthController(
            HealthDatabaseProvider healthDatabaseProvider,
            Environment environment,
            @Autowired(required = false) BuildProperties buildProperties) {
        this.healthDatabaseProvider = healthDatabaseProvider;
        this.environment = environment;
        this.buildProperties = buildProperties;
    }

    @GetMapping("/health")
    public HealthResponse response() {
        String version = buildProperties != null ? buildProperties.getVersion() : "0.0.1";
        String javaVersion = System.getProperty("java.version");
        String springBootVersion = SpringBootVersion.getVersion() != null ? SpringBootVersion.getVersion() : "unknown";
        String[] profiles = environment.getActiveProfiles();
        String currentEnv = profiles.length > 0 ? String.join(", ", profiles) : "default";

        return new HealthResponse(
            "UP",
            "PROTEC JAVA API",
            version,
            Instant.now(),
            javaVersion,
            springBootVersion,
            currentEnv,
            List.of(healthDatabaseProvider.check())
        );
    }
}
