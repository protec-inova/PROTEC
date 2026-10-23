package com.example.protec.health;

public record HealthDependency(
    String name,
    String status,
    long latency
) {}
