package com.ufpe.gateway.payment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRequest {

    private String transactionId;
    private BigDecimal amount;
    private String cardNumber;
    private String cardHolder;

}