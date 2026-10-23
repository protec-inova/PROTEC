package com.example.protec.health;

import java.time.Instant;
import java.util.List;


public record HealthResponse(
        String status,
        String service,
        String version,
        Instant timestamp,
        String javaVersion,
        String springBootVersion,
        String environment,
        List<HealthDependency> dependencies
) {}