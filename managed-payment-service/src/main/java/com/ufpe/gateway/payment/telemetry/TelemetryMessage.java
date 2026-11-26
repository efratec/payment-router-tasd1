package com.ufpe.gateway.payment.telemetry;

public record TelemetryMessage(
        String transactionId,
        String acquirer,
        long durationMs,
        boolean success
) {}