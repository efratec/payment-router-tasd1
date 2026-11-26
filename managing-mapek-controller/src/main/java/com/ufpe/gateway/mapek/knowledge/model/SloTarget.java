package com.ufpe.gateway.mapek.knowledge.model;

public record SloTarget(
        String acquirerName,
        long maxLatencyMs,
        double maxErrorRate
) {}