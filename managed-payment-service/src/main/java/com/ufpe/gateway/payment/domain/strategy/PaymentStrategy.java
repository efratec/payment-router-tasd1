package com.ufpe.gateway.payment.domain.strategy;

import com.ufpe.gateway.payment.domain.model.PaymentRequest;
import com.ufpe.gateway.payment.domain.model.PaymentResult;

public interface PaymentStrategy {

    PaymentResult processPayment(PaymentRequest request);
    String getAcquirerName();

}
