package com.ufpe.gateway.payment.domain.model;

public record PaymentResult(
        String transactionId,
        String acquirerUsed,
        boolean success,
        String authorizationCode
) {}