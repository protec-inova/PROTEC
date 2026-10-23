package com.example.protec.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * HealthDatabaseProvider
 */
@Component
public class HealthDatabaseProvider {
    private final JdbcTemplate jdbcTemplate;

    public HealthDatabaseProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HealthDependency check() {
        long startTime = System.currentTimeMillis();

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long latency = System.currentTimeMillis() - startTime;
            return new HealthDependency("Database", "UP", latency);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return new HealthDependency("Database", e.toString(), latency);
        }
    }
}
