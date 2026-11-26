package com.ufpe.gateway.mapek.monitor;

public record TelemetryEvent(
        String acquirer,
        long latencyMs,
        boolean success,
        String errorCode
) {}