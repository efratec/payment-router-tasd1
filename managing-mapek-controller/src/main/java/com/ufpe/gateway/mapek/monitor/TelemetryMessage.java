package com.ufpe.gateway.mapek.monitor;

public record TelemetryMessage(
        String acquirer,
        long latencyMs,
        boolean success,
        String errorCode,
        long timestamp
) {}